package com.example.sgmta.controllers;

import com.example.sgmta.dtos.dashboard.DashboardFiltersDTO;
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
import org.springframework.data.domain.Sort;
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

    @Operation(summary = "Métricas Globais do Dashboard", description = "Devolve as métricas agregadas baseadas no histórico recente do projeto.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas globais calculadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para aceder a este projeto")
    })
    @GetMapping("/{projectId}/dashboard/metrics/global")
    public ResponseEntity<DashboardMetricsDTO> getGlobalMetrics(
            @Parameter(description = "ID único do projeto") @PathVariable UUID projectId) {
        return ResponseEntity.ok(dashboardService.getGlobalMetrics(projectId));
    }

    @Operation(summary = "Métricas de uma Execução Específica", description = "Devolve o Health Score e detalhes de uma única build selecionada.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas da build calculadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Execução não encontrada")
    })
    @GetMapping("/{projectId}/executions/{executionId}/metrics")
    public ResponseEntity<DashboardMetricsDTO> getBuildMetrics(
            @Parameter(description = "ID único da execução (build)") @PathVariable UUID executionId) {
        return ResponseEntity.ok(dashboardService.getBuildMetrics(executionId));
    }

    @Operation(summary = "Histórico de Execuções", description = "Retorna a lista paginada de execuções com possibilidade de filtragem por branch ou versão.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de execuções recuperada com sucesso")
    })
    @GetMapping("/{projectId}/dashboard/history")
    public ResponseEntity<Page<TestExecutionSummaryDTO>> getExecutionHistory(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String versionName,
            @PageableDefault(size = 10, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TestExecutionSummaryDTO> history = dashboardService.getExecutionHistory(projectId, branchName, versionName, pageable);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Obter filtros disponíveis", description = "Retorna listas de suites e versões para popular dropdowns no frontend.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtros recuperados com sucesso")
    })
    @GetMapping("/{projectId}/dashboard/filters")
    public ResponseEntity<DashboardFiltersDTO> getFilters(@PathVariable UUID projectId) {
        return ResponseEntity.ok(dashboardService.getAvailableFilters(projectId));
    }
}