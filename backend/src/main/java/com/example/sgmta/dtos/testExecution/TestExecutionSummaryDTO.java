package com.example.sgmta.dtos.testExecution;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Schema(description = "Resumo de uma execução para a tabela de Histórico")
public record TestExecutionSummaryDTO(
        @Schema(description = "ID da execução") UUID executionId,
        @Schema(description = "Nome da build") String buildName,
        @Schema(description = "Nome da branch") String branchName,
        @Schema(description = "Nome da versão") String versionName,
        @Schema(description = "Data de início") LocalDateTime startTime,
        @Schema(description = "Duração total em milissegundos") long durationMillis,
        @Schema(description = "Indica se a execução teve pelo menos uma falha") boolean hasFailures,
        @Schema(description = "Total de testes que passaram") long passedCount,
        @Schema(description = "Total de testes que falharam") long failedCount,
        @Schema(description = "Total de testes flaky") long flakyCount
) {}