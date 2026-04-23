package com.example.sgmta.dtos.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para a criação de um novo Projeto.
 * Define as regras de validação para a entrada de dados.
 */
@Schema(description = "Estrutura de dados para criar um novo projeto")
public record ProjectCreateDTO(
        @Schema(description = "Nome do projeto", example = "Gestão de Testes")
        @NotBlank(message = "O nome do projeto é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String name,

        @Schema(description = "Descrição opcional da finalidade do projeto", example = "Testes de integração e unitários da API principal")
        @Size(max = 500, message = "A descrição não pode exceder os 500 caracteres.")
        String description,

        @Schema(description = "Penalização percentual por cada teste instável", example = "2.5")
        @DecimalMin(value = "0.0", message = "A penalização não pode ser negativa.")
        @DecimalMax(value = "15.0", message = "A penalização máxima permitida é 15%.")
        Double flakyPenalty
) {}
