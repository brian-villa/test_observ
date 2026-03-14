package com.example.sgmta.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO que representa a resposta pública de um utilizador.
 * Este objeto filtra dados sensíveis (ex: passwords) antes de serem enviados.
 */
@Schema(description = "Representação pública dos dados do utilizador")
public record UserResponseDTO(

        @Schema(description = "UUID")
        UUID id,

        @Schema(description = "Nome completo do utilizador", example = "Brian Villanova")
        String name,

        @Schema(description = "Endereço de email registado", example = "brian@example.com")
        String email
) {}
