package com.example.sgmta.integration.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.*;
import com.example.sgmta.entities.enums.TestStatus;
import com.example.sgmta.integration.config.AbstractIntegrationTest;
import com.example.sgmta.repositories.*;
import com.example.sgmta.services.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionServiceIT extends AbstractIntegrationTest {

    @Autowired private IngestionService ingestionService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private VersionRepository versionRepository;
    @Autowired private TestCaseRepository testCaseRepository;
    @Autowired private TestExecutionRepository testExecutionRepository;
    @Autowired private TestResultRepository testResultRepository;

    @Test
    @Transactional
    void shouldMarkTestAsFlakyWhenWindowContainsPassAndFail() {
        // Arrange
        // Criar Projeto
        Project project = new Project("Flaky Project", "Desc", "flaky-token-123");
        project.setFlakyThreshold(3);
        projectRepository.save(project);

        Version version = new Version("v1.0");
        versionRepository.save(version);

        TestCase testCase = new TestCase("Login Test");
        testCaseRepository.save(testCase);

        //  histórico de TestResults )
        TestExecution exec1 = new TestExecution(LocalDateTime.now().minusDays(2), "main", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2), "UI Tests", "exec-1", "Build 1", project, version);
        testExecutionRepository.save(exec1);

        TestResult result1 = new TestResult(TestStatus.FAIL, false, "Error", null, exec1, testCase);
        testResultRepository.save(result1);

        TestExecution exec2 = new TestExecution(LocalDateTime.now().minusDays(1), "main", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1), "UI Tests", "exec-2", "Build 1", project, version);
        testExecutionRepository.save(exec2);

        TestResult result2 = new TestResult(TestStatus.PASS, false, null, null, exec2, testCase);
        testResultRepository.save(result2);

        // 3. Simular o envio do 3º report que vai engatilhar a janela de 3 e marcar como flaky
        StandardizedPipelineReport.TestCaseResult item = 
            new StandardizedPipelineReport.TestCaseResult("Login Test", TestStatus.PASS, 500L, null);

        StandardizedPipelineReport report = new StandardizedPipelineReport(
                "flaky-token-123", "v1.0", "main", LocalDateTime.now(), LocalDateTime.now(), List.of(item)
        );

        // Act
        ingestionService.ingest(report, "UI Tests", "exec-3", "Build 1");

        // Assert
        List<TestResult> results = testResultRepository.findAll();
        //result criado pela ingestão
        TestResult latestResult = results.stream()
                .filter(r -> r.getTestExecution().getRunId().equals("exec-3"))
                .findFirst()
                .orElseThrow();

        assertThat(latestResult.getResult()).isEqualTo(TestStatus.PASS);
        assertThat(latestResult.getFlaky()).isTrue(); // Validou que havia FAIL e PASS na mesma janela
    }

    @Test
    @Transactional
    void shouldCreateSeparateTestCasesForDifferentProjects() {
        // Arrange: dois projetos com tokens distintos
        Project projectA = new Project("Project A", "Desc A", "token-project-a");
        projectA.setFlakyThreshold(3);
        projectRepository.save(projectA);

        Project projectB = new Project("Project B", "Desc B", "token-project-b");
        projectB.setFlakyThreshold(3);
        projectRepository.save(projectB);

        // Ambos os projetos enviam um teste com o mesmo nome
        StandardizedPipelineReport.TestCaseResult sharedNameTest =
                new StandardizedPipelineReport.TestCaseResult("Login Test", TestStatus.PASS, 100L, null);

        StandardizedPipelineReport reportA = new StandardizedPipelineReport(
                "token-project-a", "v1.0", "main",
                LocalDateTime.now(), LocalDateTime.now(), List.of(sharedNameTest)
        );
        StandardizedPipelineReport reportB = new StandardizedPipelineReport(
                "token-project-b", "v1.0", "main",
                LocalDateTime.now(), LocalDateTime.now(), List.of(sharedNameTest)
        );

        // Act
        ingestionService.ingest(reportA, "Suite", "exec-a-1", "Build 1");
        ingestionService.ingest(reportB, "Suite", "exec-b-1", "Build 1");

        // Assert: devem existir 2 TestCase separados com o mesmo nome (um por projeto)
        List<TestCase> allTestCases = testCaseRepository.findAll();
        long loginTestCount = allTestCases.stream()
                .filter(tc -> tc.getTestName().equals("Login Test"))
                .count();

        assertThat(loginTestCount).isEqualTo(2);
    }
}
