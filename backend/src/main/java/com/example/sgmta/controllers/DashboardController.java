package com.example.sgmta.controllers;

import com.example.sgmta.dtos.dashboard.DashboardFiltersDTO;
import com.example.sgmta.dtos.dashboard.DashboardMetricsDTO;
import com.example.sgmta.dtos.dashboard.FlakyGlobalDTO;
import com.example.sgmta.dtos.dashboard.SuiteAggregationDTO;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Dashboard", description = "Endpoints de métricas e visualização de dados para o Frontend")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(
        summary = "Métricas Globais do Dashboard",
        description = "Devolve as métricas agregadas da versão/suite/branch. " +
                      "Se versionName não for fornecido, usa a versão mais recente do projeto."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas globais calculadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para aceder a este projeto")
    })
    @GetMapping("/{projectId}/dashboard/metrics/global")
    public ResponseEntity<DashboardMetricsDTO> getGlobalMetrics(
            @Parameter(description = "ID único do projeto") @PathVariable UUID projectId,
            @Parameter(description = "Filtro opcional por Branch") @RequestParam(required = false) String branchName,
            @Parameter(description = "Filtro opcional por Versão (default = mais recente)") @RequestParam(required = false) String versionName,
            @Parameter(description = "Filtro opcional por Suite") @RequestParam(required = false) String suiteName) {

        return ResponseEntity.ok(dashboardService.getGlobalMetrics(projectId, branchName, versionName, suiteName));
    }

    @Operation(
        summary = "Métricas de uma Execução Específica",
        description = "Devolve o Health Score e detalhes de uma única build selecionada."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas da build calculadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Execução não encontrada")
    })
    @GetMapping("/{projectId}/executions/{executionId}/metrics")
    public ResponseEntity<DashboardMetricsDTO> getBuildMetrics(
            @Parameter(description = "ID único da execução (build)") @PathVariable UUID executionId) {
        return ResponseEntity.ok(dashboardService.getBuildMetrics(executionId));
    }

    @Operation(
        summary = "Histórico de Execuções",
        description = "Retorna a lista paginada de execuções com possibilidade de filtragem por branch, versão ou suite."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/history")
    public ResponseEntity<Page<TestExecutionSummaryDTO>> getExecutionHistory(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String versionName,
            @RequestParam(required = false) String suiteName,
            @PageableDefault(size = 10, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TestExecutionSummaryDTO> history = dashboardService.getExecutionHistory(projectId, branchName, versionName, suiteName, pageable);
        return ResponseEntity.ok(history);
    }

    @Operation(
        summary = "Obter filtros disponíveis do projeto",
        description = "Retorna listas de suites, versões e branches para popular dropdowns no frontend."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/filters")
    public ResponseEntity<DashboardFiltersDTO> getFilters(@PathVariable UUID projectId) {
        return ResponseEntity.ok(dashboardService.getAvailableFilters(projectId));
    }

    @Operation(
        summary = "Obter filtros disponíveis para uma versão específica",
        description = "Retorna as branches e suites que pertencem à versão indicada. " +
                      "Usado para garantir que os sub-filtros são coerentes com a versão selecionada."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/filters/version")
    public ResponseEntity<DashboardFiltersDTO> getFiltersForVersion(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String versionName) {
        return ResponseEntity.ok(dashboardService.getAvailableFiltersForVersion(projectId, versionName));
    }

    @Operation(
        summary = "Sumário de Versão – Pirâmide de Testes",
        description = "Devolve os totais de PASS/FAIL agrupados por suite (Unit, Integration, E2E, etc.) " +
                      "acumulados de todas as builds da versão selecionada (ou da mais recente por defeito). " +
                      "É o dado principal para o gráfico de Pirâmide de Testes."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/version-summary")
    public ResponseEntity<List<SuiteAggregationDTO>> getVersionSummary(
            @PathVariable UUID projectId,
            @Parameter(description = "Nome da versão (default = mais recente)") @RequestParam(required = false) String versionName,
            @Parameter(description = "Filtro opcional por Branch") @RequestParam(required = false) String branchName,
            @Parameter(description = "Filtro opcional por Suite") @RequestParam(required = false) String suiteName) {

        return ResponseEntity.ok(dashboardService.getVersionSummary(projectId, versionName, branchName, suiteName));
    }

    @Operation(
        summary = "Obter Flakys da Versão",
        description = "Retorna todos os testes instáveis activos na versão indicada (ou mais recente por defeito), " +
                      "filtráveis por branch e suite."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/flaky")
    public ResponseEntity<List<FlakyGlobalDTO>> getGlobalFlakyTests(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String versionName,
            @RequestParam(required = false) String suiteName) {
        return ResponseEntity.ok(dashboardService.getGlobalFlakyTests(projectId, branchName, versionName, suiteName));
    }

    @Operation(
        summary = "Pesquisa Global de Testes",
        description = "Procura testes pelo nome no projeto e devolve o seu último estado dentro dos filtros aplicados."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{projectId}/dashboard/search")
    public ResponseEntity<List<com.example.sgmta.dtos.testResult.TestResultResponseDTO>> searchTests(
            @PathVariable UUID projectId,
            @Parameter(description = "Texto a pesquisar no nome do teste") @RequestParam String query,
            @Parameter(description = "Filtro opcional por Branch") @RequestParam(required = false) String branchName,
            @Parameter(description = "Filtro opcional por Versão") @RequestParam(required = false) String versionName,
            @Parameter(description = "Filtro opcional por Suite") @RequestParam(required = false) String suiteName) {

        return ResponseEntity.ok(dashboardService.searchTests(projectId, query, branchName, versionName, suiteName));
    }
}