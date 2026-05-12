package com.example.sgmta.integration.repositories;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.Version;
import com.example.sgmta.integration.config.AbstractIntegrationTest;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.VersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionRepositoryIT extends AbstractIntegrationTest {

    @Autowired private TestExecutionRepository testExecutionRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private VersionRepository versionRepository;

    @Test
    @Transactional
    void shouldFindDistinctSuiteNamesByProjectId() {
        // Arrange
        Project project = new Project("Test Project", "Desc", "t-123");
        projectRepository.save(project);

        Version version = new Version("v1.0");
        versionRepository.save(version);

        // Criamos 3 execuções: 2 com a mesma Suite, 1 com Suite diferente
        TestExecution exec1 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "UI-Tests", "r1", project, version);
        TestExecution exec2 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "UI-Tests", "r2", project, version);
        TestExecution exec3 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "API-Tests", "r3", project, version);
        
        testExecutionRepository.saveAll(List.of(exec1, exec2, exec3));

        // Act
        List<String> distinctSuites = testExecutionRepository.findDistinctSuiteNamesByProjectId(project.getId());

        // Assert
        assertThat(distinctSuites)
                .hasSize(2)
                .containsExactlyInAnyOrder("UI-Tests", "API-Tests");
    }

    @Test
    @Transactional
    void shouldFindFilteredHistory() {
        // Arrange
        Project project = new Project("Test Project", "Desc", "t-123");
        projectRepository.save(project);

        Version v1 = new Version("v1.0");
        Version v2 = new Version("v2.0");
        versionRepository.saveAll(List.of(v1, v2));

        TestExecution exec1 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "Suite", "r1", project, v1);
        TestExecution exec2 = new TestExecution(LocalDateTime.now(), "develop", LocalDateTime.now(), LocalDateTime.now(), "Suite", "r2", project, v1);
        TestExecution exec3 = new TestExecution(LocalDateTime.now(), "main", LocalDateTime.now(), LocalDateTime.now(), "Suite", "r3", project, v2);
        
        testExecutionRepository.saveAll(List.of(exec1, exec2, exec3));

        Pageable pageable = PageRequest.of(0, 10);

        // Act 1: Sem filtros extras (apenas ProjectId)
        Page<TestExecution> allExecs = testExecutionRepository.findFilteredHistory(project.getId(), null, null, pageable);
        assertThat(allExecs.getContent()).hasSize(3);

        // Act 2: Filtrar apenas por branch 'main'
        Page<TestExecution> mainExecs = testExecutionRepository.findFilteredHistory(project.getId(), "main", null, pageable);
        assertThat(mainExecs.getContent()).hasSize(2);
        assertThat(mainExecs.getContent()).extracting("branchName").containsOnly("main");

        // Act 3: Filtrar por branch 'main' e versão 'v1.0'
        Page<TestExecution> specificExecs = testExecutionRepository.findFilteredHistory(project.getId(), "main", "v1.0", pageable);
        assertThat(specificExecs.getContent()).hasSize(1);
        assertThat(specificExecs.getContent().get(0).getRunId()).isEqualTo("r1");
    }
}
