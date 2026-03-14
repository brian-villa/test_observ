package com.example.sgmta.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para atualização do perfil do utilizador")
public record UserUpdateDTO(
        @Schema(example = "xpto", description = "Novo nome do utilizador")
        @NotBlank(message = "O nome não pode estar vazio")
        String name
) {
}
