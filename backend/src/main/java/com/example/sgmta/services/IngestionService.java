package com.example.sgmta.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
public class IngestionService {

    private final ProjectRepository projectRepository;
    private final VersionService versionService;
    private final TestCaseService testCaseService;
    private final TestExecutionService testExecutionService;
    private final TestResultService testResultService;
    private final TestExecutionRepository testExecutionRepository;

    public IngestionService(ProjectRepository projectRepository, VersionService versionService,
                            TestCaseService testCaseService, TestExecutionService testExecutionService,
                            TestResultService testResultService, TestExecutionRepository testExecutionRepository) {
        this.projectRepository = projectRepository;
        this.versionService = versionService;
        this.testCaseService = testCaseService;
        this.testExecutionService = testExecutionService;
        this.testResultService = testResultService;
        this.testExecutionRepository = testExecutionRepository;
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

            testResultService.createResult(item.status(), execution, testCase);

            if ("FAIL".equalsIgnoreCase(item.status())) {
                checkAndMarkFlaky(testCase, project);
            }
        }
    }

    private void checkAndMarkFlaky(TestCase testCase, Project project) {
        long failureCount = testResultService.countFailures(testCase, project);

        if (failureCount >= project.getFlakyThreshold()) {
            testCase.setFlaky(true);
            testCaseService.save(testCase);
        }
    }
}