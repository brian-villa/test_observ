package com.example.sgmta.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class IngestionService {

    private final ProjectRepository projectRepository;
    private final VersionService versionService;
    private final TestCaseService testCaseService;
    private final TestExecutionService testExecutionService;
    private final TestResultService testResultService;
    private final TestExecutionRepository testExecutionRepository;

    private final TestResultRepository testResultRepository;

    public IngestionService(ProjectRepository projectRepository, VersionService versionService,
                            TestCaseService testCaseService, TestExecutionService testExecutionService,
                            TestResultService testResultService, TestExecutionRepository testExecutionRepository,
                            TestResultRepository testResultRepository) {
        this.projectRepository = projectRepository;
        this.versionService = versionService;
        this.testCaseService = testCaseService;
        this.testExecutionService = testExecutionService;
        this.testResultService = testResultService;
        this.testExecutionRepository = testExecutionRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Processa a ingestão, agrupando testes na mesma Execução com base no Projeto + Suite + Execution ID.
     */
    @Transactional
    public void ingest(StandardizedPipelineReport report, String suiteName, String executionId) {

        Project project = projectRepository.findByProjectToken(report.projectToken())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado para o token fornecido."));

        Version version = versionService.findOrCreate(report.versionName());

        Optional<TestExecution> existingExecOpt = testExecutionRepository
                .findTopByProjectIdAndSuiteNameAndRunId(project.getId(), suiteName, executionId);

        TestExecution execution;

        if (existingExecOpt.isPresent()) {
            execution = existingExecOpt.get();

            if (report.startTime() != null && report.endTime() != null) {
                long payloadDurationMillis = Duration.between(report.startTime(), report.endTime()).toMillis();
                if (payloadDurationMillis > 0) {
                    execution.setEndTime(execution.getEndTime().plusNanos(payloadDurationMillis * 1_000_000));
                    execution = testExecutionRepository.save(execution);
                }
            }
        } else {
            execution = testExecutionService.createExecution(
                    project,
                    version,
                    report.branchName(),
                    report.startTime(),
                    report.endTime(),
                    suiteName,
                    executionId
            );
        }

        for (StandardizedPipelineReport.TestCaseResult item : report.tests()) {
            TestCase testCase = testCaseService.findOrCreate(item.testName());
            String cleanedError = cleanErrorMessage(item.errorMessage());
            TestResult currentResult = testResultService.createResult(item.status(), cleanedError, execution, testCase);
            checkAndMarkFlaky(testCase, project, currentResult);
        }
    }

    /**
     *
     * Guarda o estado diretamente no TestResult.
     */
    private void checkAndMarkFlaky(TestCase testCase, Project project, TestResult currentResult) {
        int threshold = project.getFlakyThreshold();

        if (threshold <= 0) {
            return;
        }

        int windowSize = Math.max(15, threshold * 3);

        List<TestResult> recentResults = testResultRepository.findRecentResultsByTestCaseAndProject(
                testCase.getId(),
                project.getId(),
                PageRequest.of(0, windowSize, Sort.by(Sort.Direction.DESC, "testExecution.startTime"))
        ).getContent();

        long failCount = 0;
        boolean hasPass = false;

        for (TestResult r : recentResults) {
            if ("FAIL".equalsIgnoreCase(r.getResult())) {
                failCount++;
            } else if ("PASS".equalsIgnoreCase(r.getResult())) {
                hasPass = true;
            }
        }

        boolean isNowFlaky = (failCount >= threshold && hasPass);

        if (isNowFlaky) {
            currentResult.setFlaky(true);
            testResultService.save(currentResult);
        }
    }

    private String cleanErrorMessage(String rawError) {
        if (rawError == null || rawError.isBlank()) return null;

        if (rawError.startsWith("{") && rawError.contains("message=")) {
            try {
                String clean = rawError;
                if (clean.contains("message=")) {
                    clean = clean.substring(clean.indexOf("message=") + 8);
                }
                // Remove o rasto do mapeamento se existir
                clean = clean.replace("type=", "\nType: ")
                        .replace(", =", "\n\nStack Trace:\n")
                        .replace("}", "");

                return clean.trim();
            } catch (Exception e) {
                return rawError;
            }
        }
        return rawError;
    }
}