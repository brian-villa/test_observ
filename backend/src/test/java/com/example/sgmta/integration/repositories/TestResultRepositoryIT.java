package com.example.sgmta.integration.repositories;

import com.example.sgmta.entities.*;
import com.example.sgmta.entities.enums.TestStatus;
import com.example.sgmta.integration.config.AbstractIntegrationTest;
import com.example.sgmta.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultRepositoryIT extends AbstractIntegrationTest {

    @Autowired private TestResultRepository testResultRepository;
    @Autowired private TestExecutionRepository testExecutionRepository;
    @Autowired private TestCaseRepository testCaseRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private VersionRepository versionRepository;

    @Test
    @Transactional
    void shouldFindFilteredResults() {
        // Arrange
        Project project = new Project("Test Project", "Desc", "t-123");
        projectRepository.save(project);

        Version version = new Version("v1.0");
        versionRepository.save(version);

        TestExecution exec = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "Suite", "r1", "Build 1", project, version);
        testExecutionRepository.save(exec);

        TestCase tc1 = new TestCase("Login Success");
        TestCase tc2 = new TestCase("Login Failure");
        TestCase tc3 = new TestCase("Logout Test");
        testCaseRepository.saveAll(List.of(tc1, tc2, tc3));

        TestResult r1 = new TestResult(TestStatus.PASS, false, null, null, exec, tc1);
        TestResult r2 = new TestResult(TestStatus.FAIL, false, "Error 1", null, exec, tc2);
        TestResult r3 = new TestResult(TestStatus.FAIL, true, "Error Flaky", null, exec, tc3);
        testResultRepository.saveAll(List.of(r1, r2, r3));

        Pageable pageable = PageRequest.of(0, 10);

        // Act 1: Search por texto "Login" e status NULL (antigo ALL)
        Page<TestResult> searchLogin = testResultRepository.findFilteredResults(exec.getId(), "login", null, null, pageable);
        assertThat(searchLogin.getContent()).hasSize(2); // Deve apanhar tc1 e tc2

        // Act 2: Status FAIL sem pesquisa de texto
        Page<TestResult> failsOnly = testResultRepository.findFilteredResults(exec.getId(), null, TestStatus.FAIL, null, pageable);
        assertThat(failsOnly.getContent()).hasSize(2);

        // Act 3: Apenas testes Flaky
        Page<TestResult> flakysOnly = testResultRepository.findFilteredResults(exec.getId(), null, null, true, pageable);
        assertThat(flakysOnly.getContent()).hasSize(1);
        assertThat(flakysOnly.getContent().get(0).getTestCase().getTestName()).isEqualTo("Logout Test");
    }

    @Test
    @Transactional
    void shouldFindActiveFlakyTestsByProjectId() {
        // Arrange
        Project project = new Project("Flaky Project", "Desc", "t-123");
        projectRepository.save(project);

        Version version = new Version("v1.0");
        versionRepository.save(version);

        TestCase tc = new TestCase("Flaky Test Case");
        testCaseRepository.save(tc);

        // Execução 1 (Mais antiga)
        TestExecution exec1 = new TestExecution(LocalDateTime.now().minusDays(2), "main", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2), "Suite", "r1", "Build 1", project, version);
        testExecutionRepository.save(exec1);
        TestResult r1 = new TestResult(TestStatus.FAIL, true, "Error", null, exec1, tc); // Antigo era flaky
        testResultRepository.save(r1);

        // Execução 2 (Mais recente)
        TestExecution exec2 = new TestExecution(LocalDateTime.now().minusDays(1), "main", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1), "Suite", "r2", "Build 1", project, version);
        testExecutionRepository.save(exec2);
        
        // Se a execução mais recente não for flaky (ex: PASS e isFlaky=false), a query não deve retornar este TestCase
        TestResult r2 = new TestResult(TestStatus.PASS, false, null, null, exec2, tc);
        testResultRepository.save(r2);

        // Act 1: Como o mais recente é false, a query não deve retornar nada
        List<TestResult> activeFlakys1 = testResultRepository.findActiveFlakyTestsByProjectId(project.getId());
        assertThat(activeFlakys1).isEmpty();

        // Arrange 2: Inserir uma execução ainda mais recente onde o teste volta a ser Flaky
        TestExecution exec3 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "Suite", "r3", "Build 1", project, version);
        testExecutionRepository.save(exec3);
        TestResult r3 = new TestResult(TestStatus.PASS, true, null, null, exec3, tc);
        testResultRepository.save(r3);

        // Act 2: Agora o mais recente é Flaky, deve ser retornado
        List<TestResult> activeFlakys2 = testResultRepository.findActiveFlakyTestsByProjectId(project.getId());
        assertThat(activeFlakys2).hasSize(1);
        assertThat(activeFlakys2.get(0).getId()).isEqualTo(r3.getId());
    }
}
