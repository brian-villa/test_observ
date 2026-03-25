package com.example.sgmta.repositories;

import com.example.sgmta.entities.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para a entidade Version.
 * Responsável por gerir a persistência das versões do software testado.
 */
@Repository
public interface VersionRepository extends JpaRepository<Version, UUID> {

    /**
     * Procura uma versão exata pelo seu nome.
     * Utilizado durante a ingestão de dados (RF.05) para evitar a duplicação
     * de versões (ex: reaproveitar a versão "v1.0" se ela já existir na base de dados).
     *
     * @param versionName O nome da versão enviada pela pipeline.
     * @return Um Optional contendo a Versão, se encontrada.
     */

    Optional<Version> findByVersionName(String versionName);
}
