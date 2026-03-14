package com.example.sgmta.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registo de um novo utilizador no sistema")
public record RegisterDTO(
        @Schema(example = "XPTO", description = "Nome completo do utilizador")
        @NotBlank(message = "O nome não pode estar vazio.")
        String name,

        @Schema(example = "xpto@example.com", description = "Endereço de email único")
        @NotBlank(message = "O email não pode estar vazio.")
        @Email(message = "O formato do email é inválido.")
        String email,

        @Schema(example = "Password123", description = "Palavra-passe (mínimo 8 caracteres)")
        @NotBlank(message = "A senha não pode estar vazia.")
        @Size(min = 8, message = "A password tem que ter pelo menos 8 caracteres")
        String password
) {}
