package com.example.sgmta.services;

import com.example.sgmta.dtos.dashboard.DashboardMetricsDTO;
import com.example.sgmta.dtos.dashboard.FlakyTestSummaryDTO;
import com.example.sgmta.dtos.dashboard.TestFailureSummaryDTO;
import com.example.sgmta.dtos.testExecution.TestExecutionSummaryDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.repositories.TestCaseRepository;
import com.example.sgmta.repositories.TestExecutionRepository;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;
    private final TestCaseRepository testCaseRepository;

    public DashboardService(ProjectRepository projectRepository,
                            TestExecutionRepository testExecutionRepository,
                            TestResultRepository testResultRepository,
                            TestCaseRepository testCaseRepository) {
        this.projectRepository = projectRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testResultRepository = testResultRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDTO getProjectMetrics(UUID projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        long totalExecutions = testExecutionRepository.countByProjectId(projectId);

        if (totalExecutions == 0) {
            return new DashboardMetricsDTO(project.getName(), 0, 0, 0, "Sem execuções", new ArrayList<>(), new ArrayList<>());
        }

        int healthScore = 0;
        String lastExecutionTime = "Desconhecido";
        List<TestFailureSummaryDTO> recentFailures = new ArrayList<>();

        var lastExecutionOpt = testExecutionRepository.findTopByProjectIdOrderByStartTimeDesc(projectId);

        if (lastExecutionOpt.isPresent()) {
            TestExecution lastExec = lastExecutionOpt.get();
            lastExecutionTime = formatTimeAgo(lastExec.getStartTime());

            // Contar sucessos e falhas desta execução
            long passed = testResultRepository.countByTestExecutionIdAndResult(lastExec.getId(), "PASS");
            long failed = testResultRepository.countByTestExecutionIdAndResult(lastExec.getId(), "FAIL");
            long total = passed + failed;

            if (total > 0) {
                healthScore = (int) Math.round(((double) passed / total) * 100);
            }

            recentFailures = testResultRepository.findByTestExecutionIdAndResult(lastExec.getId(), "FAIL")
                    .stream()
                    .limit(10)
                    .map(r -> new TestFailureSummaryDTO(r.getId(), r.getTestCase().getTestName(), r.getResult()))
                    .collect(Collectors.toList());
        }

        long totalFlaky = testCaseRepository.countByFlakyTrue();
        List<FlakyTestSummaryDTO> flakyTests = testCaseRepository.findByFlakyTrue()
                .stream()
                .limit(5)
                .map(t -> new FlakyTestSummaryDTO(t.getId(), t.getTestName(), "Alta"))
                .collect(Collectors.toList());

        return new DashboardMetricsDTO(
                project.getName(),
                healthScore,
                totalExecutions,
                totalFlaky,
                lastExecutionTime,
                recentFailures,
                flakyTests
        );
    }

    private String formatTimeAgo(LocalDateTime startTime) {
        if (startTime == null) return "Data desconhecida";
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        if (duration.toMinutes() < 1) return "Agora mesmo";
        if (duration.toMinutes() < 60) return "Há " + duration.toMinutes() + " minutos";
        if (duration.toHours() < 24) return "Há " + duration.toHours() + " horas";
        return "Há " + duration.toDays() + " dias";
    }

    /**
     * Devolve o histórico paginado e filtrado, mapeando as Entidades para DTOs.
     */
    @Transactional(readOnly = true)
    public Page<TestExecutionSummaryDTO> getExecutionHistory(
            UUID projectId, String branchName, String versionName, Pageable pageable) {

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