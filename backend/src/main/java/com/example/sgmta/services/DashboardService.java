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

    @Transactional(readOnly = true)
    public DashboardMetricsDTO getGlobalMetrics(UUID projectId, String branchName, String versionName, String suiteName) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        long totalExecutions = testExecutionRepository.countByProjectId(projectId);

        if (totalExecutions == 0) {
            return new DashboardMetricsDTO(project.getName(), 0, 0, 0, "Sem execuções", new ArrayList<>(), new ArrayList<>());
        }

        List<TestExecution> recentExecutions = testExecutionRepository.findFilteredHistory(
                projectId, branchName, versionName, suiteName, PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "startTime"))).getContent();

        if (recentExecutions.isEmpty()) {
            return new DashboardMetricsDTO(project.getName(), 0, totalExecutions, 0, "Desconhecido", new ArrayList<>(), new ArrayList<>());
        }

        long totalPassedGlobal = 0;
        long totalTestsGlobal = 0;

        for (TestExecution exec : recentExecutions) {
            long passed = testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "PASS");
            long failed = testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "FAIL");
            totalPassedGlobal += passed;
            totalTestsGlobal += (passed + failed);
        }

        List<TestResult> activeFlakys = testResultRepository.findActiveFlakyTestsByProjectId(projectId);
        if (suiteName != null && !suiteName.isBlank()) {
            activeFlakys = activeFlakys.stream()
                    .filter(f -> suiteName.equalsIgnoreCase(f.getTestExecution().getSuiteName()))
                    .collect(Collectors.toList());
        }
        // Filtra por Branch
        if (branchName != null && !branchName.isBlank()) {
            activeFlakys = activeFlakys.stream()
                    .filter(f -> branchName.equalsIgnoreCase(f.getTestExecution().getBranchName()))
                    .collect(Collectors.toList());
        }

        // Filtra por Versão
        if (versionName != null && !versionName.isBlank()) {
            activeFlakys = activeFlakys.stream()
                    .filter(f -> f.getTestExecution().getVersion() != null &&
                            versionName.equalsIgnoreCase(f.getTestExecution().getVersion().getVersionName()))
                    .collect(Collectors.toList());
        }

        // Filtra por Suite
        if (suiteName != null && !suiteName.isBlank()) {
            activeFlakys = activeFlakys.stream()
                    .filter(f -> suiteName.equalsIgnoreCase(f.getTestExecution().getSuiteName()))
                    .collect(Collectors.toList());
        }

        long totalFlakysGlobais = activeFlakys.size();

        int globalHealthScore = 0;
        if (totalTestsGlobal > 0) {
            double baseSuccessRate = ((double) totalPassedGlobal / totalTestsGlobal) * 100.0;
            double penaltyPerFlaky = project.getFlakyPenalty() != null ? project.getFlakyPenalty() : 2.5;
            double flakyPenaltyTotal = totalFlakysGlobais * penaltyPerFlaky;

            globalHealthScore = (int) Math.max(0, Math.round(baseSuccessRate - flakyPenaltyTotal));
        }

        TestExecution lastExec = recentExecutions.get(0);
        String lastExecutionTime = formatTimeAgo(lastExec.getStartTime());

        List<TestFailureSummaryDTO> globalRecentFailures = getFailuresForExecution(lastExec.getId());
        List<FlakyTestSummaryDTO> globalFlakyTests = getFlakysForExecution(lastExec.getId());

        return new DashboardMetricsDTO(
                project.getName(),
                globalHealthScore,
                totalExecutions,
                totalFlakysGlobais,
                lastExecutionTime,
                globalRecentFailures,
                globalFlakyTests
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

        long activeFlakysInBuild = testResultRepository.countByTestExecutionIdAndFlakyTrue(execution.getId());

        int buildHealthScore = 0;
        if (total > 0) {
            double baseSuccessRate = ((double) passed / total) * 100.0;

            double penalty = execution.getProject().getFlakyPenalty() != null ? execution.getProject().getFlakyPenalty() : 2.5;
            double flakyPenalty = activeFlakysInBuild * penalty;
            buildHealthScore = (int) Math.max(0, Math.round(baseSuccessRate - flakyPenalty));
        }

        String executionTime = formatTimeAgo(execution.getStartTime());

        List<TestFailureSummaryDTO> buildFailures = getFailuresForExecution(execution.getId());
        List<FlakyTestSummaryDTO> buildFlakys = getFlakysForExecution(execution.getId());

        return new DashboardMetricsDTO(
                projectName,
                buildHealthScore,
                1,
                activeFlakysInBuild,
                executionTime,
                buildFailures,
                buildFlakys
        );
    }

    private String formatTimeAgo(LocalDateTime startTime) {
        if (startTime == null) return "Data desconhecida";
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        if (duration.toMinutes() < 1) return "Agora mesmo";
        if (duration.toMinutes() < 60) return "Há " + duration.toMinutes() + " minutos";
        if (duration.toHours() < 24) return "Há " + duration.toHours() + " horas";
        return "Há " + duration.toDays() + " dias";
    }

    @Transactional(readOnly = true)
    public Page<TestExecutionSummaryDTO> getExecutionHistory(
            UUID projectId, String branchName, String versionName, String suiteName, Pageable pageable) {

        Page<TestExecution> executionPage = testExecutionRepository
                .findFilteredHistory(projectId, branchName, versionName, suiteName, pageable);

        return executionPage.map(execution -> {
            long durationMillis = 0L;
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                durationMillis = java.time.Duration.between(
                        execution.getStartTime(),
                        execution.getEndTime()
                ).toMillis();
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
                    testResultRepository.countByTestExecutionIdAndFlakyTrue(execution.getId())

            );
        });
    }

    public DashboardFiltersDTO getAvailableFilters(UUID projectId) {
        List<String> branches = testExecutionRepository.findDistinctBranchNamesByProjectId(projectId);
        List<String> suites = testExecutionRepository.findDistinctSuiteNamesByProjectId(projectId);
        List<String> versions = testExecutionRepository.findDistinctVersionNamesByProjectId(projectId);
        return new DashboardFiltersDTO(suites, versions, branches);
    }

    private List<TestFailureSummaryDTO> getFailuresForExecution(UUID execId) {
        return testResultRepository.findByTestExecutionIdAndResult(execId, "FAIL").stream()
                .limit(10)
                .map(r -> new TestFailureSummaryDTO(r.getId(), r.getTestCase().getTestName(), r.getResult()))
                .collect(Collectors.toList());
    }

    private List<FlakyTestSummaryDTO> getFlakysForExecution(UUID execId) {
        return testResultRepository.findByTestExecutionIdAndFlakyTrue(execId).stream()
                .limit(5)
                .map(r -> new FlakyTestSummaryDTO(r.getId(), r.getTestCase().getTestName(), "Alta"))
                .collect(Collectors.toList());
    }

    public List<FlakyGlobalDTO> getGlobalFlakyTests(UUID projectId, String branchName, String versionName, String suiteName) {

        List<com.example.sgmta.entities.TestResult> latestFlakys =
                testResultRepository.findActiveFlakyTestsByProjectId(projectId);

        if (branchName != null && !branchName.isBlank()) {
            latestFlakys = latestFlakys.stream()
                    .filter(f -> branchName.equalsIgnoreCase(f.getTestExecution().getBranchName()))
                    .collect(Collectors.toList());
        }

        if (versionName != null && !versionName.isBlank()) {
            latestFlakys = latestFlakys.stream()
                    .filter(f -> f.getTestExecution().getVersion() != null &&
                            versionName.equalsIgnoreCase(f.getTestExecution().getVersion().getVersionName()))
                    .collect(Collectors.toList());
        }

        if (suiteName != null && !suiteName.isBlank()) {
            latestFlakys = latestFlakys.stream()
                    .filter(f -> suiteName.equalsIgnoreCase(f.getTestExecution().getSuiteName()))
                    .collect(Collectors.toList());
        }

        return latestFlakys.stream()
                .map(r -> new FlakyGlobalDTO(
                        r.getId(),
                        r.getTestCase().getTestName(),
                        r.getTestExecution().getId()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Pesquisa testes por nome e devolve o DTO com o resultado mais recente,
     * permitindo navegação rápida no Frontend.
     */
    @Transactional(readOnly = true)
    public List<TestResultResponseDTO> searchTests(UUID projectId, String searchTerm, String branchName, String versionName, String suiteName) {
        if (searchTerm == null || searchTerm.trim().isBlank()) {
            return new ArrayList<>();
        }

        List<TestResult> results = testResultRepository.searchLatestResultsByTestName(
                projectId,
                searchTerm.trim(),
                branchName,
                suiteName,
                versionName,
                PageRequest.of(0, 10)
        );

        return results.stream()
                .map(com.example.sgmta.mappers.TestResultMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}