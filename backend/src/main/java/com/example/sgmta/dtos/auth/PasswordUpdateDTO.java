package com.example.sgmta.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alteração de password")
public record PasswordUpdateDTO(
        @Schema(example = "Antiga123!", description = "A password que o utilizador usa atualmente")
        @NotBlank(message = "Password atual é obrigatória")
        String oldPassword,

        @Schema(example = "Nova456!", description = "A nova password (mínimo 8 caracteres)")
        @NotBlank(message = "Nova password é obrigatória")
        @Size(min = 8, message = "A nova password deve ter pelo menos 8 caracteres")
        String newPassword
) {}
