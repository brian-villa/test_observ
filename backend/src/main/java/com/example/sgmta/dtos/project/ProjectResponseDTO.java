package com.example.sgmta.dtos.project;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa a resposta pública de um Projeto.
 * Transporta a API Key para que o utilizador a possa configurar no pipeline.
 */
@Schema(description = "Representação dos dados de um projeto devolvidos pela API")
public record ProjectResponseDTO(
        @Schema(description = "Identificador único do projeto", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Nome do projeto", example = "Gestão de Testes")
        String name,

        @Schema(description = "Descrição do projeto", example = "Testes de integração e unitários da API principal")
        String description,

        @Schema(description = "Chave para API Key", example = "sgmta_9f86d081884c7d659a2feaa0c55ad015a")
        String projectToken,

        @Schema(description = "Data exata em que o projeto foi criado no sistema")
        LocalDateTime createdAt,

        @Schema(description = "Quantidade de transições de estado (PASS/FAIL) toleradas antes de marcar um teste como Flaky", example = "3")
        Integer flakyThreshold,

        @Schema(description = "Penalização percentual por cada teste instável")
        Double flakyPenalty
) {}
