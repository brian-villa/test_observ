package com.example.sgmta.dtos.testResult;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Estrutura de dados de resposta para o resultado de um teste individual")
public record TestResultResponseDTO(
        @Schema(description = "ID único do resultado", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Status final do teste", example = "PASS")
        String result,

        @Schema(description = "Nome do caso de teste avaliado", example = "shouldAuthenticateUser")
        String testCaseName,

        @Schema(description = "Indica se o teste tem um histórico de instabilidade", example = "false")
        Boolean isFlaky,

        @Schema(description = "ID da execução (pipeline) à qual este resultado pertence")
        UUID testExecutionId,

        @Schema(description = "Justificação do porquê de o teste ser considerado flaky")
        String flakyReason,

        @Schema(description = "Mensagem de erro ou stack trace caso o teste tenha falhado", example = "java.lang.AssertionError: expected [200] but found [404]")
        String errorMessage,

        @Schema(description = "Imagem da falha em formato Base64")
        String screenshotBase64
) {}