package com.example.sgmta.controllers;

import com.example.sgmta.dtos.project.ProjectCreateDTO;
import com.example.sgmta.dtos.project.ProjectResponseDTO;
import com.example.sgmta.dtos.project.ProjectUpdateDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.User;
import com.example.sgmta.mappers.ProjectMapper;
import com.example.sgmta.services.ProjectService;
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

import java.util.UUID;

/**
 * Controller responsável pela gestão do ciclo de vida dos projetos.
 * Protegido por autenticação JWT.
 */
@RestController
@RequestMapping("/projects")
@Tag(name = "Projetos", description = "Endpoints para criação, edição e gestão de API Keys de projetos")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Criar novo projeto", description = "Gera um projeto e a sua respetiva API Key estática.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou nome duplicado")
    })

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(
            @Valid @RequestBody ProjectCreateDTO data,
            @AuthenticationPrincipal User user) {

        Project project = projectService.create(data, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toResponseDTO(project));
    }

    @Operation(summary = "Obter projeto por ID", description = "Retorna os detalhes e a API Key de um projeto específico.")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getById(@PathVariable UUID id) {
        Project project = projectService.findById(id);
        return ResponseEntity.ok(ProjectMapper.toResponseDTO(project));
    }

    @Operation(summary = "Atualizar projeto", description = "Atualização parcial (PATCH) do nome ou descrição.")
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateDTO data) {

        Project project = projectService.update(id, data);
        return ResponseEntity.ok(ProjectMapper.toResponseDTO(project));
    }

    @Operation(summary = "Gerar nova API Key", description = "Muda a chave de integração. A chave antiga deixará de funcionar imediatamente.")
    @PatchMapping("/{id}/rotate-token")
    public ResponseEntity<ProjectResponseDTO> rotateToken(@PathVariable UUID id) {
        Project project = projectService.rotateToken(id);
        return ResponseEntity.ok(ProjectMapper.toResponseDTO(project));
    }

    @Operation(summary = "Apagar projeto", description = "Remove o projeto e desvincula os utilizadores associados.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
