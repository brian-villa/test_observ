package com.example.sgmta.mappers;

import com.example.sgmta.dtos.version.VersionResponseDTO;
import com.example.sgmta.entities.Version;

/**
 * Classe utilitária responsável pelo mapeamento entre Entidades e DTO de Version  .
 * Isola a lógica de conversão para garantir a reutilização de código.
 */
public class VersionMapper {

    public static VersionResponseDTO toResponseDTO(Version version) {
        if (version == null) {
            return null;
        }
        return new VersionResponseDTO(
                version.getId(),
                version.getVersionName()
        );
    }
}
