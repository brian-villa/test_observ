package com.example.sgmta.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para alteração de email")
public record EmailUpdateDTO(
        @Schema(example = "novo@exemplo.com", description = "O novo endereço de email para login")
        @NotBlank(message = "O novo email é obrigatório")
        @Email(message = "Formato de email inválido")
        String newEmail
) {}
