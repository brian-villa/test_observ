package com.example.sgmta.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.repositories.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IngestionService {

    private final ProjectRepository projectRepository;
    private final VersionService versionService;
    private final TestCaseService testCaseService;
    private final TestExecutionService testExecutionService;
    private final TestResultService testResultService;

    public IngestionService(ProjectRepository projectRepository, VersionService versionService,
                            TestCaseService testCaseService, TestExecutionService testExecutionService,
                            TestResultService testResultService) {
        this.projectRepository = projectRepository;
        this.versionService = versionService;
        this.testCaseService = testCaseService;
        this.testExecutionService = testExecutionService;
        this.testResultService = testResultService;
    }

    /**
     * Processa a ingestão massiva de dados, orquestrando a criação de todas as entidades relacionadas.
     */
    @Transactional
    public void ingest(StandardizedPipelineReport report) {

        Project project = projectRepository.findByProjectToken(report.projectToken())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado para o token fornecido."));

        Version version = versionService.findOrCreate(report.versionName());

        TestExecution execution = testExecutionService.createExecution(
                project,
                version,
                report.branchName(),
                report.startTime(),
                report.endTime()
        );

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