package com.example.sgmta.dtos.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Métricas agregadas e listas de dados para popular a página de Análise do Dashboard")
public record DashboardMetricsDTO(

        @Schema(description = "Nome do projeto a ser exibido no painel", example = "Autenticação Core")
        String projectName,

        @Schema(description = "Taxa de sucesso da última execução em percentagem (0 a 100)", example = "98")
        int healthScore,

        @Schema(description = "Número total de execuções de pipelines realizadas ao longo do tempo", example = "142")
        long totalExecutions,

        @Schema(description = "Quantidade de Casos de Teste atualmente marcados como instáveis (Flaky)", example = "15")
        long totalFlaky,

        @Schema(description = "Tempo formatado desde a última execução (calculado pelo backend)", example = "Há 10 minutos")
        String lastExecutionTime,

        @Schema(description = "Lista resumo das falhas ocorridas na execução mais recente para preenchimento de tabelas")
        List<TestFailureSummaryDTO> recentFailures,

        @Schema(description = "Lista resumo dos testes com maior taxa de instabilidade no projeto")
        List<FlakyTestSummaryDTO> flakyTests
) {}