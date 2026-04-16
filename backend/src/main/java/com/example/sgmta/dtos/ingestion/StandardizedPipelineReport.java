package com.example.sgmta.dtos.ingestion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Independentemente do formato de entrada ou da plataforma,
 * os dados são  normalizados para este formato antes de chegarem ao Service.
 */
@Schema(description = "Relatório padronizado de pipeline, independente da ferramenta de origem.")
public record StandardizedPipelineReport(

        @Schema(description = "Token de autenticação e identificação do Projeto", example = "sgmta_12345abcde")
        String projectToken,

        @Schema(description = "Nome da versão do software a ser testada", example = "v1.2.0")
        String versionName,

        @Schema(description = "Nome da branch do repositório onde a pipeline correu", example = "feature/login")
        String branchName,

        @Schema(description = "Timestamp do início da execução da pipeline", example = "2026-03-17T09:55:00")
        LocalDateTime startTime,

        @Schema(description = "Timestamp do fim da execução da pipeline", example = "2026-03-17T10:05:00")
        LocalDateTime endTime,

        @Schema(description = "Lista massiva contendo os resultados individuais de cada teste")
        List<TestCaseResult> tests
) {
    @Schema(description = "Sub-estrutura transitória que representa um único teste dentro do relatório de ingestão")
    public record TestCaseResult(

            @Schema(description = "Nome do caso de teste", example = "shouldAuthenticateUser")
            String testName,

            @Schema(description = "Status final da execução do teste", example = "PASS")
            String status,

            @Schema(description = "Duração da execução do teste em milissegundos", example = "120")
            Long durationMs,

            @Schema(description = "A stack trace ou motivo da falha do teste", example = "NullPointerException at line 45")
            String errorMessage
    ) {}
}
