package com.example.sgmta.dtos.testExecution;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Estrutura de dados de resposta para o histórico de uma Execução")
public record TestExecutionResponseDTO(
        @Schema(description = "ID único da execução", example = "123e4567-a11b-12a3-a456-123456789012")
        UUID id,

        @Schema(description = "Nome do projeto onde a execução ocorreu", example = "Gestão de Testes")
        String projectName,

        @Schema(description = "Versão do software testada", example = "v1.2.0")
        String versionName,

        @Schema(description = "Nome da branch", example = "feature/login")
        String branchName,

        @Schema(description = "Data de registo da execução", example = "2026-03-17T10:00:00")
        LocalDateTime executionDate,

        @Schema(description = "Início do pipeline", example = "2026-03-17T09:55:00")
        LocalDateTime startTime,

        @Schema(description = "Fim do pipeline", example = "2026-03-17T10:05:00")
        LocalDateTime endTime
) {}