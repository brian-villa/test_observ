package com.example.sgmta.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais necessárias para autenticação no sistema")
public record LoginDTO(
        @Schema(example = "xpto@example.com", description = "Email do utilizador registado")
        @NotBlank(message = "O email não pode estar vazio")
        @Email(message = "O formato do email é inválido")
        String email,

        @Schema(example = "Password123", description = "Palavra-passe do utilizador")
        @NotBlank(message = "A password não pode estar vazia")
        String password
) {}
