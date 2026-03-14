package com.example.sgmta.controllers;

import com.example.sgmta.dtos.user.UserResponseDTO;
import com.example.sgmta.dtos.user.UserUpdateDTO;
import com.example.sgmta.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller responsável pela manutenção de perfis de utilizador.
 * Todos os endpoints requerem autenticação via JWT.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Utilizadores", description = "Endpoints para consulta e manutenção de perfis")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Listar utilizadores", description = "Retorna a lista de todos os utilizadores (apenas dados públicos).")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        var users = userService.findAll().stream()
                .map(u -> new UserResponseDTO(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Obter utilizador por ID", description = "Recupera os detalhes de um utilizador específico pelo UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilizador encontrado"),
            @ApiResponse(responseCode = "404", description = "Utilizador não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable UUID id) {
        var user = userService.findById(id);
        return ResponseEntity.ok(new UserResponseDTO(user.getId(), user.getName(), user.getEmail()));
    }

    @Operation(summary = "Atualizar nome", description = "Permite alterar o nome do perfil de um utilizador.")
    @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid UserUpdateDTO data) {
        var user = userService.update(id, data);
        return ResponseEntity.ok(new UserResponseDTO(user.getId(), user.getName(), user.getEmail()));
    }

    @Operation(summary = "Eliminar conta", description = "Remove permanentemente o utilizador do sistema.")
    @ApiResponse(responseCode = "204", description = "Conta eliminada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
