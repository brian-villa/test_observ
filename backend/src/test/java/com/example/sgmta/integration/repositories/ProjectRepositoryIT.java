package com.example.sgmta.integration.repositories;

import com.example.sgmta.entities.Project;
import com.example.sgmta.integration.config.AbstractIntegrationTest;
import com.example.sgmta.repositories.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldSaveAndRetrieveProject() {
        // Arrange
        Project project = new Project("Test Project", "Integration Test Description", "test-token-123");
        
        // Act
        Project saved = projectRepository.save(project);
        boolean exists = projectRepository.existsByName("Test Project");
        
        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(exists).isTrue();
    }
}
