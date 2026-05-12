package com.example.sgmta.unit.services;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.Version;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import com.example.sgmta.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private VersionService versionService;
    @Mock private TestCaseService testCaseService;
    @Mock private TestExecutionService testExecutionService;
    @Mock private TestResultService testResultService;
    @Mock private TestExecutionRepository testExecutionRepository;
    @Mock private TestResultRepository testResultRepository;

    @InjectMocks
    private IngestionService ingestionService;

    @Test
    void shouldThrowExceptionWhenProjectTokenIsInvalid() {
        // Arrange
        StandardizedPipelineReport report = new StandardizedPipelineReport(
                "invalid-token", "v1", "main", LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
        when(projectRepository.findByProjectToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingest(report, "UI Tests", "exec-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Projeto não encontrado para o token fornecido.");
    }

    @Test
    void shouldCreateNewTestExecutionWhenItDoesNotExist() {
        // Arrange
        String token = "valid-token";
        Project project = new Project("Proj", "Desc", token);
        Version version = new Version("v1");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(5);
        
        StandardizedPipelineReport report = new StandardizedPipelineReport(
                token, "v1", "main", start, end, List.of()
        );

        when(projectRepository.findByProjectToken(token)).thenReturn(Optional.of(project));
        when(versionService.findOrCreate("v1")).thenReturn(version);
        when(testExecutionRepository.findTopByProjectIdAndSuiteNameAndRunId(project.getId(), "UI Tests", "exec-123"))
                .thenReturn(Optional.empty());

        // Act
        ingestionService.ingest(report, "UI Tests", "exec-123");

        // Assert
        verify(testExecutionService).createExecution(
                project, version, "main", start, end, "UI Tests", "exec-123"
        );
    }

    @Test
    void shouldUpdateEndTimeWhenTestExecutionAlreadyExists() {
        // Arrange
        String token = "valid-token";
        Project project = new Project("Proj", "Desc", token);
        Version version = new Version("v1");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(5);

        StandardizedPipelineReport report = new StandardizedPipelineReport(
                token, "v1", "main", start, end, List.of()
        );

        TestExecution existingExecution = mock(TestExecution.class);
        when(existingExecution.getEndTime()).thenReturn(start);

        when(projectRepository.findByProjectToken(token)).thenReturn(Optional.of(project));
        when(versionService.findOrCreate("v1")).thenReturn(version);
        when(testExecutionRepository.findTopByProjectIdAndSuiteNameAndRunId(project.getId(), "UI Tests", "exec-123"))
                .thenReturn(Optional.of(existingExecution));

        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(existingExecution);

        // Act
        ingestionService.ingest(report, "UI Tests", "exec-123");

        // Assert
        verify(testExecutionService, never()).createExecution(any(), any(), any(), any(), any(), any(), any());
        verify(testExecutionRepository).save(existingExecution);
    }
}
