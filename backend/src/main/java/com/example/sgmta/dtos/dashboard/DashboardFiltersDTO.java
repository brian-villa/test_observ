package com.example.sgmta.dtos.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Filtragem de atributos a serem utilizados para ter uma visão historial das suite de testes")
public record DashboardFiltersDTO(
        @Schema(description = "Lista de suite de testes executadas", example = "Backend Unit Tests")
        List<String> suites,
        @Schema(description = "Lista de versões executadas", example = "build 107")
        List<String> versions
) {
}
