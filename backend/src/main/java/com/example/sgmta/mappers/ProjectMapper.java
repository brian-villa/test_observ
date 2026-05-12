package com.example.sgmta.mappers;

import com.example.sgmta.dtos.project.ProjectResponseDTO;
import com.example.sgmta.entities.Project;

/**
 * Classe utilitária responsável pelo mapeamento entre Entidades e DTO de Projetos  .
 * Isola a lógica de conversão para garantir a reutilização de código.
 */
public class ProjectMapper {

    /**
     * Converte a entidade Project num DTO para exposição na API.
     */
    public static ProjectResponseDTO toResponseDTO(Project project) {
        if (project == null) {
            return null;
        }

        return new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectToken(),
                project.getCreatedAt(),
                project.getFlakyThreshold(),
                project.getFlakyPenalty()
        );
    }
}
