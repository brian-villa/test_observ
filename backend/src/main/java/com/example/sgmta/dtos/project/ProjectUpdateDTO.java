package com.example.sgmta.dtos.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização parcial de um Projeto.
 */
@Schema(description = "Estrutura de dados para atualizar um projeto existente")
public record ProjectUpdateDTO(
        @Schema(description = "Novo nome do projeto", example = "Gestão de Projetos")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String name,

        @Schema(description = "Nova descrição do projeto", example = "Nova arquitetura otimizada")
        @Size(max = 500, message = "A descrição não pode exceder os 500 caracteres.")
        String description,

        @Schema(description = "Nova tolerância de transições para considerar um teste como Flaky", example = "5")
        @Min(value = 1, message = "A tolerância mínima deve ser 1 transição.")
                Integer flakyThreshold
) {}
