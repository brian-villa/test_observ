package com.example.sgmta.dtos.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO que padroniza o formato de resposta para qualquer erro da API.
 */
@Schema(description = "Estrutura padronizada para mensagens de erro da API")
public record StandardErrorDTO(
        @Schema(example = "2026-03-14T20:31:22", description = "Registo do momento do erro")
        LocalDateTime timestamp,

        @Schema(example = "400", description = "Código de estado HTTP")
        Integer status,

        @Schema(example = "Utilizador não encontrado", description = "Mensagem descritiva do erro")
        String message,

        @Schema(example = "/users/123e4567-e89b-12d3-a456-426614174000", description = "Caminho da requisição que gerou o erro")
        String path
) {}
