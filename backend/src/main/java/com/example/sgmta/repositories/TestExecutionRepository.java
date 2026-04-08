package com.example.sgmta.repositories;

import com.example.sgmta.entities.TestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    /**
     * Conta o total de execuções de um projeto específico para a plataforma.
     */
    long countByProjectId(UUID projectId);

    /**
     * Busca as execuções de um projeto com filtros opcionais de branch e versão, devolvendo de forma paginada.
     */
    @Query("SELECT te FROM TestExecution te WHERE te.project.id = :projectId " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:versionName IS NULL OR te.version.versionName = :versionName)")
    Page<TestExecution> findFilteredHistory(
            @Param("projectId") UUID projectId,
            @Param("branchName") String branchName,
            @Param("versionName") String versionName,
            Pageable pageable);
}
