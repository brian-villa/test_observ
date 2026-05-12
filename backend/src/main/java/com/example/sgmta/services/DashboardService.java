package com.example.sgmta.services;

import com.example.sgmta.dtos.dashboard.*;
import com.example.sgmta.dtos.testExecution.TestExecutionSummaryDTO;
import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;

    public DashboardService(ProjectRepository projectRepository,
                            TestExecutionRepository testExecutionRepository,
                            TestResultRepository testResultRepository) {
        this.projectRepository = projectRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Resolve a versão mais recente do projeto se nenhuma versão for fornecida.
     * Garante que a saúde global tem sempre um contexto de versão claro.
     */
    private String resolveEffectiveVersion(UUID projectId, String versionName) {
        if (versionName != null && !versionName.isBlank()) {
            return versionName;
        }
        List<String> latest = testExecutionRepository.findLatestVersionNameByProjectId(
                projectId, PageRequest.of(0, 1));
        return latest.isEmpty() ? null : latest.get(0);
    }

    private String formatTimeAgo(LocalDateTime startTime) {
        if (startTime == null) return "Data desconhecida";
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        if (duration.toMinutes() < 1) return "Agora mesmo";
        if (duration.toMinutes() < 60) return "Há " + duration.toMinutes() + " minutos";
        if (duration.toHours() < 24) return "Há " + duration.toHours() + " horas";
        return "Há " + duration.toDays() + " dias";
    }

    private List<TestFailureSummaryDTO> getFailuresByVersion(UUID projectId, String versionName, String branchName, String suiteName) {
        return testResultRepository
                .findRecentFailuresByVersion(projectId, versionName, branchName, suiteName, PageRequest.of(0, 10))
                .stream()
                .map(r -> new TestFailureSummaryDTO(r.getId(), r.getTestCase().getTestName(), r.getResult()))
                .collect(Collectors.toList());
    }

    private List<FlakyTestSummaryDTO> getFlakyByVersion(UUID projectId, String versionName, String branchName, String suiteName) {
        return testResultRepository
                .findActiveFlakyTestsByVersion(projectId, versionName, branchName, suiteName)
                .stream()
                .limit(10)
                .map(r -> new FlakyTestSummaryDTO(r.getId(), r.getTestCase().getTestName(), "Alta"))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDTO getGlobalMetrics(UUID projectId, String branchName, String versionName, String suiteName) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        long totalExecutions = testExecutionRepository.countByProjectId(projectId);

        if (totalExecutions == 0) {
            return new DashboardMetricsDTO(project.getName(), 0, 0, 0, "Sem execuções", new ArrayList<>(), new ArrayList<>());
        }

        String effectiveVersion = resolveEffectiveVersion(projectId, versionName);

        long filteredExecutions = testExecutionRepository
                .findFilteredHistory(projectId, branchName, effectiveVersion, suiteName,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "startTime")))
                .getTotalElements();

        if (filteredExecutions == 0) {
            return new DashboardMetricsDTO(project.getName(), 0, totalExecutions, 0, "Desconhecido", new ArrayList<>(), new ArrayList<>());
        }

        List<Object[]> passFailList = testResultRepository.sumPassFailByVersion(projectId, effectiveVersion, branchName, suiteName);

        Object[] passFail = (passFailList != null && !passFailList.isEmpty()) ? passFailList.get(0) : null;

        long totalPassed = passFail != null && passFail[0] != null ? ((Number) passFail[0]).longValue() : 0L;
        long totalFailed = passFail != null && passFail[1] != null ? ((Number) passFail[1]).longValue() : 0L;
        long totalTests = totalPassed + totalFailed;

        long totalFlakys = testResultRepository.countActiveFlakyByVersion(projectId, effectiveVersion, branchName, suiteName);

        int healthScore = 0;
        if (totalTests > 0) {
            double baseSuccessRate = ((double) totalPassed / totalTests) * 100.0;

            double penaltyPerFlaky = project.getFlakyPenalty() != null ? project.getFlakyPenalty() : 2.5;

            healthScore = (int) Math.max(0, Math.round(baseSuccessRate - (totalFlakys * penaltyPerFlaky)));
        }

        // Última execução dentro do filtro para o tempo relativo
        List<TestExecution> lastExecList = testExecutionRepository
                .findFilteredHistory(projectId, branchName, effectiveVersion, suiteName,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "startTime")))
                .getContent();
        String lastExecutionTime = lastExecList.isEmpty() ? "Desconhecido" : formatTimeAgo(lastExecList.get(0).getStartTime());

        List<TestFailureSummaryDTO> recentFailures = getFailuresByVersion(projectId, effectiveVersion, branchName, suiteName);
        List<FlakyTestSummaryDTO> flakyTests = getFlakyByVersion(projectId, effectiveVersion, branchName, suiteName);

        return new DashboardMetricsDTO(
                project.getName(),
                healthScore,
                filteredExecutions,
                totalFlakys,
                lastExecutionTime,
                recentFailures,
                flakyTests
        );
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDTO getBuildMetrics(UUID executionId) {

        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execução não encontrada"));

        String projectName = execution.getProject().getName();

        long passed = testResultRepository.countByTestExecutionIdAndResult(execution.getId(), "PASS");
        long failed = testResultRepository.countByTestExecutionIdAndResult(execution.getId(), "FAIL");
        long total = passed + failed;

        long activeFlakysInBuild = testResultRepository.countDistinctFlakyByTestExecutionId(execution.getId());

        int buildHealthScore = 0;
        if (total > 0) {
            double baseSuccessRate = ((double) passed / total) * 100.0;
            double penalty = execution.getProject().getFlakyPenalty() != null ? execution.getProject().getFlakyPenalty() : 2.5;
            buildHealthScore = (int) Math.max(0, Math.round(baseSuccessRate - activeFlakysInBuild * penalty));
        }

        String executionTime = formatTimeAgo(execution.getStartTime());

        List<TestFailureSummaryDTO> buildFailures = testResultRepository
                .findByTestExecutionIdAndResult(execution.getId(), "FAIL")
                .stream().limit(10)
                .map(r -> new TestFailureSummaryDTO(r.getId(), r.getTestCase().getTestName(), r.getResult()))
                .collect(Collectors.toList());

        List<FlakyTestSummaryDTO> buildFlakys = testResultRepository
                .findByTestExecutionIdAndFlakyTrue(execution.getId())
                .stream().limit(5)
                .map(r -> new FlakyTestSummaryDTO(r.getId(), r.getTestCase().getTestName(), "Alta"))
                .collect(Collectors.toList());

        return new DashboardMetricsDTO(projectName, buildHealthScore, 1, activeFlakysInBuild, executionTime, buildFailures, buildFlakys);
    }

    @Transactional(readOnly = true)
    public Page<TestExecutionSummaryDTO> getExecutionHistory(
            UUID projectId, String branchName, String versionName, String suiteName, Pageable pageable) {

        Page<TestExecution> executionPage = testExecutionRepository
                .findFilteredHistory(projectId, branchName, versionName, suiteName, pageable);

        return executionPage.map(execution -> {
            long durationMillis = 0L;
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                durationMillis = java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
            }
            String resolvedVersionName = execution.getVersion() != null ? execution.getVersion().getVersionName() : "N/A";
            boolean hasFailures = testResultRepository.existsByTestExecutionIdAndResult(execution.getId(), "FAIL");

            return new TestExecutionSummaryDTO(
                    execution.getId(),
                    execution.getBuildName(),
                    execution.getBranchName(),
                    resolvedVersionName,
                    execution.getStartTime(),
                    durationMillis,
                    hasFailures,
                    testResultRepository.countByTestExecutionIdAndResult(execution.getId(), "PASS"),
                    testResultRepository.countByTestExecutionIdAndResult(execution.getId(), "FAIL"),
                    testResultRepository.countDistinctFlakyByTestExecutionId(execution.getId()),
                    execution.getSuiteName(),
                    execution.getRunId()
            );
        });
    }

    /**
     * Devolve os filtros disponíveis para o projeto.
     * Quando versionName é fornecido, branches e suites são filtradas por essa versão.
     */
    public DashboardFiltersDTO getAvailableFilters(UUID projectId) {
        List<String> branches = testExecutionRepository.findDistinctBranchNamesByProjectId(projectId);
        List<String> suites = testExecutionRepository.findDistinctSuiteNamesByProjectId(projectId);
        List<String> versions = testExecutionRepository.findDistinctVersionNamesByProjectId(projectId);
        return new DashboardFiltersDTO(suites, versions, branches);
    }

    /**
     * Devolve os filtros dependentes de uma versão específica (branches e suites dessa versão).
     */
    public DashboardFiltersDTO getAvailableFiltersForVersion(UUID projectId, String versionName) {
        List<String> versions = testExecutionRepository.findDistinctVersionNamesByProjectId(projectId);
        List<String> branches = testExecutionRepository.findDistinctBranchNamesByProjectIdAndVersion(projectId, versionName);
        List<String> suites = testExecutionRepository.findDistinctSuiteNamesByProjectIdAndVersion(projectId, versionName);
        return new DashboardFiltersDTO(suites, versions, branches);
    }

    /**
     * Devolve os totais agregados por suite dentro de uma versão.
     * É o dado principal para a Pirâmide de Testes no dashboard.
     */
    @Transactional(readOnly = true)
    public List<SuiteAggregationDTO> getVersionSummary(UUID projectId, String versionName, String branchName, String suiteName) {
        String effectiveVersion = resolveEffectiveVersion(projectId, versionName);

        List<Object[]> rows = testResultRepository.aggregateResultsBySuiteAndVersion(
                projectId, effectiveVersion, branchName, suiteName);

        return rows.stream().map(row -> {
            String suite = (String) row[0];
            long passed = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long failed = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long buildCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            return new SuiteAggregationDTO(suite, passed, failed, passed + failed, buildCount);
        }).collect(Collectors.toList());
    }

    public List<FlakyGlobalDTO> getGlobalFlakyTests(UUID projectId, String branchName, String versionName, String suiteName) {
        String effectiveVersion = resolveEffectiveVersion(projectId, versionName);

        return testResultRepository
                .findActiveFlakyTestsByVersion(projectId, effectiveVersion, branchName, suiteName)
                .stream()
                .map(r -> new FlakyGlobalDTO(r.getId(), r.getTestCase().getTestName(), r.getTestExecution().getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestResultResponseDTO> searchTests(UUID projectId, String searchTerm, String branchName, String versionName, String suiteName) {
        if (searchTerm == null || searchTerm.trim().isBlank()) {
            return new ArrayList<>();
        }
        List<TestResult> results = testResultRepository.searchLatestResultsByTestName(
                projectId, searchTerm.trim(), branchName, suiteName, versionName, PageRequest.of(0, 10));

        return results.stream()
                .map(com.example.sgmta.mappers.TestResultMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}