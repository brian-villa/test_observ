package com.example.sgmta.dtos.version;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Estrutura de dados de resposta para uma Versão")
public record VersionResponseDTO(
        @Schema(description = "ID único da versão", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Nome descritivo da versão", example = "v1.0.5-release")
        String versionName
) {}
