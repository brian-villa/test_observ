package com.example.sgmta.controllers;

import com.example.sgmta.dtos.version.VersionResponseDTO;
import com.example.sgmta.entities.Version;
import com.example.sgmta.mappers.VersionMapper;
import com.example.sgmta.services.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;
import java.util.List;

/**
 * Controller responsável pela exposição de versões.
 */
@RestController
@RequestMapping("/versions")
public class VersionController {
    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @Operation(summary = "Listar todas as versões", description = "Retorna o catálogo completo de versões registadas. Útil para filtros de Dashboard.")
    @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
    @GetMapping
    public ResponseEntity<List<VersionResponseDTO>> findAll() {
        List<Version> versions = versionService.findAll();

        List<VersionResponseDTO> response = versions.stream()
                .map(VersionMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
