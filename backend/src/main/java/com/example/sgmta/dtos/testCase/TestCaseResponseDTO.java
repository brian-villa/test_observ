package com.example.sgmta.dtos.testCase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Estrutura de dados de resposta para um Caso de Teste")
public record TestCaseResponseDTO(
        @Schema(description = "ID único do caso de teste", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Nome descritivo do teste", example = "shouldAuthenticateUserWithValidCredentials")
        String testName,

        @Schema(description = "Indicador de instabilidade (Flaky).", example = "false")
        Boolean flaky
) {
}
