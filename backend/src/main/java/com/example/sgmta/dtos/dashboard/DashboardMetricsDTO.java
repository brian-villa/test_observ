package com.example.sgmta.dtos.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Métricas agregadas para os gráficos do Dashboard")
public record DashboardMetricsDTO(

        @Schema(description = "Número total de execuções de pipelines realizadas", example = "142")
        long totalExecutions,

        @Schema(description = "Quantidade de testes individuais que passaram com sucesso", example = "8500")
        long totalPassedTests,

        @Schema(description = "Quantidade de testes individuais que falharam", example = "320")
        long totalFailedTests,

        @Schema(description = "Quantidade de Casos de Teste atualmente marcados como instáveis (Flaky)", example = "15")
        long totalFlakyTests
) {}