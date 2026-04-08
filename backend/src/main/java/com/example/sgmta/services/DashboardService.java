package com.example.sgmta.services;

import com.example.sgmta.dtos.dashboard.DashboardMetricsDTO;
import com.example.sgmta.dtos.testExecution.TestExecutionSummaryDTO;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DashboardService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;

    public DashboardService(TestExecutionRepository testExecutionRepository, TestResultRepository testResultRepository) {
        this.testExecutionRepository = testExecutionRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Reúne todas as métricas agregadas de um projeto para o Dashboard.
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDTO getProjectMetrics(UUID projectId) {
        long totalExecutions = testExecutionRepository.countByProjectId(projectId);

        // Se não houver execuções, devolve tudo a zero para evitar queries desnecessárias
        if (totalExecutions == 0) {
            return new DashboardMetricsDTO(0, 0, 0, 0);
        }

        long totalPassed = testResultRepository.countByTestExecutionProjectIdAndResult(projectId, "PASS");
        long totalFailed = testResultRepository.countByTestExecutionProjectIdAndResult(projectId, "FAIL");
        long totalFlaky = testResultRepository.countFlakyTestsByProjectId(projectId);

        return new DashboardMetricsDTO(totalExecutions, totalPassed, totalFailed, totalFlaky);
    }

    /**
     * Devolve o histórico paginado e filtrado, mapeando as Entidades para DTOs.
     */
    @Transactional(readOnly = true)
    public Page<TestExecutionSummaryDTO> getExecutionHistory(
            UUID projectId, String branchName, String versionName, Pageable pageable) {

        // 1. Vai buscar a página de entidades com base nos filtros
        Page<TestExecution> executionPage = testExecutionRepository
                .findFilteredHistory(projectId, branchName, versionName, pageable);

        // Entidade para o DTO de resumo
        return executionPage.map(execution -> {

            // duração em minutos
            long durationMinutes = 0L;
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                durationMinutes = java.time.Duration.between(
                        execution.getStartTime(),
                        execution.getEndTime()
                ).toMinutes();
            }

            // Vai buscar o nome da versão
            String resolvedVersionName = execution.getVersion() != null ? execution.getVersion().getVersionName() : "N/A";

            // Verifica na tabela TestResult se houve alguma falha nesta execução
            boolean hasFailures = testResultRepository.existsByTestExecutionIdAndResult(execution.getId(), "FAIL");

            return new TestExecutionSummaryDTO(
                    execution.getId(),
                    execution.getBranchName(),
                    resolvedVersionName,
                    execution.getStartTime(),
                    durationMinutes,
                    hasFailures
            );
        });
    }
}