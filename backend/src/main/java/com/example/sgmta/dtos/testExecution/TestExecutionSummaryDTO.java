package com.example.sgmta.dtos.testExecution;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resumo de uma execução para a tabela de Histórico")
public record TestExecutionSummaryDTO(
        @Schema(description = "ID da execução", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID executionId,

        @Schema(description = "Nome da branch", example = "feature/login")
        String branchName,

        @Schema(description = "Nome da versão", example = "v1.2.0")
        String versionName,

        @Schema(description = "Data de início", example = "2026-04-08T18:00:00")
        LocalDateTime startTime,

        @Schema(description = "Duração total em milissegundos", example = "15400")
        long durationMillis,

        @Schema(description = "Indica se a execução teve pelo menos uma falha", example = "true")
        boolean hasFailures
) {}