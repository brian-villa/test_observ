package com.example.sgmta.controllers;

import com.example.sgmta.dtos.auth.EmailUpdateDTO;
import com.example.sgmta.dtos.auth.LoginDTO;
import com.example.sgmta.dtos.auth.PasswordUpdateDTO;
import com.example.sgmta.dtos.auth.RegisterDTO;
import com.example.sgmta.entities.User;
import com.example.sgmta.services.AuthService;
import com.example.sgmta.services.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Operações de acesso ao sistema")

public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Criar conta", description = "Cria um novo utilizador e cifra a password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilizador registado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou email duplicado")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO data) {
        authService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registado com sucesso!");
    }

    @Operation(summary = "Login", description = "Valida credenciais e devolve um Token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Email ou senha incorretos")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginDTO data) {
        var user = authService.login(data);
        var token = tokenService.generateToken(user);
        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Alterar Password", description = "Permite ao utilizador autenticado mudar a sua password.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password alterada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Password atual incorreta ou token inválido")
    })
    @PatchMapping("/update-password")
    public ResponseEntity<String> updatePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid PasswordUpdateDTO data) {

        authService.updatePassword(user.getEmail(), data.oldPassword(), data.newPassword());
        return ResponseEntity.ok("Password atualizada com sucesso.");
    }

    @Operation(summary = "Alterar Email", description = "Permite ao utilizador autenticado mudar o seu email de login.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Novo email já está em uso"),
            @ApiResponse(responseCode = "403", description = "Token inválido")
    })
    @PatchMapping("/update-email")
    public ResponseEntity<String> updateEmail(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid EmailUpdateDTO data) {

        authService.updateEmail(user.getEmail(), data.newEmail());
        return ResponseEntity.ok("Email atualizado. Use o novo email no próximo login.");
    }
}
