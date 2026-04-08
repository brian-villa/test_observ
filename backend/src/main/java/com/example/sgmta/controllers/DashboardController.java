package com.example.sgmta.controllers;

import com.example.sgmta.dtos.dashboard.DashboardMetricsDTO;
import com.example.sgmta.dtos.testExecution.TestExecutionSummaryDTO;
import com.example.sgmta.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Dashboard", description = "Endpoints de métricas e visualização de dados para o Frontend")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Métricas do Dashboard", description = "Devolve as métricas globais e agregadas de um projeto específico.")
    @SecurityRequirement(name = "bearerAuth") // Protegido pelo JWT que já implementaste
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas calculadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para aceder a este projeto")
    })
    @GetMapping("/{projectId}/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics(
            @Parameter(description = "ID único do projeto") @PathVariable UUID projectId) {

        // TODO Futuro: Validar se o User autenticado pertence realmente a este projectId

        DashboardMetricsDTO metrics = dashboardService.getProjectMetrics(projectId);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Histórico de Execuções", description = "Retorna a lista paginada de execuções com possibilidade de filtragem por branch ou versão.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de execuções recuperada com sucesso")
    })
    @GetMapping("/{projectId}/dashboard/history")
    public ResponseEntity<Page<TestExecutionSummaryDTO>> getExecutionHistory(
            @Parameter(description = "ID do projeto") @PathVariable UUID projectId,
            @Parameter(description = "Filtro opcional pelo nome da branch") @RequestParam(required = false) String branchName,
            @Parameter(description = "Filtro opcional pela versão") @RequestParam(required = false) String versionName,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "startTime,desc") Pageable pageable) {

        // Ocultamos o parâmetro Pageable do Swagger (@Parameter(hidden = true)) porque o
        // Swagger tem dificuldade em renderizá-lo corretamente, mas ele funciona na URL via ?page=0&size=10

        Page<TestExecutionSummaryDTO> history = dashboardService.getExecutionHistory(projectId, branchName, versionName, pageable);

        return ResponseEntity.ok(history);
    }
}