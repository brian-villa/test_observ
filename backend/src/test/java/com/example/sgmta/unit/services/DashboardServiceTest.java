package com.example.sgmta.unit.services;

import com.example.sgmta.dtos.dashboard.DashboardMetricsDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import com.example.sgmta.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TestExecutionRepository testExecutionRepository;
    @Mock private TestResultRepository testResultRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnDefaultMetricsWhenProjectHasNoExecutions() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Empty Project", "Desc", "token");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(testExecutionRepository.countByProjectId(projectId)).thenReturn(0L);

        // Act
        DashboardMetricsDTO result = dashboardService.getGlobalMetrics(projectId);

        // Assert
        assertThat(result.projectName()).isEqualTo("Empty Project");
        assertThat(result.healthScore()).isZero();
        assertThat(result.totalExecutions()).isZero();
        assertThat(result.lastExecutionTime()).isEqualTo("Sem execuções");
    }

    @Test
    void shouldCalculatePerfectHealthScoreWithNoFlakys() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Perfect Project", "Desc", "token");
        TestExecution exec = mock(TestExecution.class);
        when(exec.getId()).thenReturn(UUID.randomUUID());
        when(exec.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(testExecutionRepository.countByProjectId(projectId)).thenReturn(1L);
        when(testExecutionRepository.findFilteredHistory(eq(projectId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exec)));
        
        // Simular 10 passed e 0 failed (100% success rate)
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "PASS")).thenReturn(10L);
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "FAIL")).thenReturn(0L);
        
        // Simular 0 flakys globais
        when(testResultRepository.findActiveFlakyTestsByProjectId(projectId)).thenReturn(List.of());

        // Act
        DashboardMetricsDTO result = dashboardService.getGlobalMetrics(projectId);

        // Assert
        assertThat(result.healthScore()).isEqualTo(100); // 100% - 0
        assertThat(result.totalExecutions()).isEqualTo(1L);
    }

    @Test
    void shouldApplyFlakyPenaltyToHealthScore() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Flaky Project", "Desc", "token");
        TestExecution exec = mock(TestExecution.class);
        when(exec.getId()).thenReturn(UUID.randomUUID());
        when(exec.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(testExecutionRepository.countByProjectId(projectId)).thenReturn(1L);
        when(testExecutionRepository.findFilteredHistory(eq(projectId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exec)));
        
        // Simular 10 passed e 0 failed (100% success rate base)
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "PASS")).thenReturn(10L);
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "FAIL")).thenReturn(0L);
        
        // Simular 4 flakys globais (penalty de 4 * 2.5 = 10)
        TestResult flaky1 = mock(TestResult.class);
        TestResult flaky2 = mock(TestResult.class);
        TestResult flaky3 = mock(TestResult.class);
        TestResult flaky4 = mock(TestResult.class);
        
        // Precisamos mockar os TestCases para evitar NullPointer em getGlobalMetrics -> getTestCase().getTestName()
        // Wait, activeFlakys é usado apenas para .size() no getGlobalMetrics
        // Porém, testResultRepository.findByTestExecutionIdAndFlakyTrue(execId) é chamado
        // Let's configure return mocks correctly.
        when(testResultRepository.findActiveFlakyTestsByProjectId(projectId)).thenReturn(List.of(flaky1, flaky2, flaky3, flaky4));

        // Act
        DashboardMetricsDTO result = dashboardService.getGlobalMetrics(projectId);

        // Assert
        // 100% base - (4 * 2.5) = 90
        assertThat(result.healthScore()).isEqualTo(90);
    }

    @Test
    void shouldNotReturnNegativeHealthScore() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Bad Project", "Desc", "token");
        TestExecution exec = mock(TestExecution.class);
        when(exec.getId()).thenReturn(UUID.randomUUID());
        when(exec.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(testExecutionRepository.countByProjectId(projectId)).thenReturn(1L);
        when(testExecutionRepository.findFilteredHistory(eq(projectId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exec)));
        
        // Simular 1 passed e 9 failed (10% success rate)
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "PASS")).thenReturn(1L);
        when(testResultRepository.countByTestExecutionIdAndResult(exec.getId(), "FAIL")).thenReturn(9L);
        
        // Simular 10 flakys globais (penalty de 10 * 2.5 = 25)
        // 10% - 25 = -15 -> Deve ser travado em 0
        List<TestResult> flakys = mock(List.class);
        when(flakys.size()).thenReturn(10);
        when(testResultRepository.findActiveFlakyTestsByProjectId(projectId)).thenReturn(flakys);

        // Act
        DashboardMetricsDTO result = dashboardService.getGlobalMetrics(projectId);

        // Assert
        assertThat(result.healthScore()).isEqualTo(0); // Travado no 0
    }

    @Test
    void shouldThrowExceptionWhenProjectIsNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getGlobalMetrics(projectId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Projeto não encontrado");
    }
}
