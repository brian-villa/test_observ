package com.example.sgmta.services;

import com.example.sgmta.dtos.dashboard.*;
import com.example.sgmta.dtos.testExecution.TestExecutionSummaryDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public DashboardMetricsDTO getGlobalMetrics(UUID projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        long totalExecutions = testExecutionRepository.countByProjectId(projectId);

        if (totalExecutions == 0) {
            return new DashboardMetricsDTO(project.getName(), 0, 0, 0, "Sem execuções", new ArrayList<>(), new ArrayList<>());
        }

        List<TestExecution> recentExecutions = testExecutionRepository.findFilteredHistory(
                projectId, null, null, PageRequest.of(0, 15)).getContent();

        //erros se a lista vier vazia
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
        long totalFlakysGlobais = activeFlakys.size();

        int globalHealthScore = 0;
        if (totalTestsGlobal > 0) {
            double baseSuccessRate = ((double) totalPassedGlobal / totalTestsGlobal) * 100.0;
            double flakyPenalty = totalFlakysGlobais * 2.5;
            globalHealthScore = (int) Math.max(0, Math.round(baseSuccessRate - flakyPenalty));
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
            double flakyPenalty = activeFlakysInBuild * 2.5;
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
            UUID projectId, String branchName, String versionName, Pageable pageable) {

        Page<TestExecution> executionPage = testExecutionRepository
                .findFilteredHistory(projectId, branchName, versionName, pageable);

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
        List<String> suites = testExecutionRepository.findDistinctSuiteNamesByProjectId(projectId);
        List<String> versions = testExecutionRepository.findDistinctVersionNamesByProjectId(projectId);

        return new DashboardFiltersDTO(suites, versions);
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

    public List<FlakyGlobalDTO> getGlobalFlakyTests(UUID projectId) {

        // Pega apenas a "fotografia" mais recente que esteja instável
        List<com.example.sgmta.entities.TestResult> latestFlakys =
                testResultRepository.findActiveFlakyTestsByProjectId(projectId);

        return latestFlakys.stream()
                .map(r -> new FlakyGlobalDTO(
                        r.getId(),
                        r.getTestCase().getTestName(),
                        r.getTestExecution().getId()
                ))
                .collect(Collectors.toList());
    }
}