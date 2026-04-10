package com.example.sgmta.repositories;

import com.example.sgmta.entities.TestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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
    @Query(value = "SELECT te FROM TestExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:versionName IS NULL OR v.versionName = :versionName)",
            countQuery = "SELECT count(te) FROM TestExecution te " +
                    "LEFT JOIN te.version v " +
                    "WHERE te.project.id = :projectId " +
                    "AND (:branchName IS NULL OR te.branchName = :branchName) " +
                    "AND (:versionName IS NULL OR v.versionName = :versionName)")
    Page<TestExecution> findFilteredHistory(
            @Param("projectId") UUID projectId,
            @Param("branchName") String branchName,
            @Param("versionName") String versionName,
            Pageable pageable);

    /**
     * Busca a última execução de um projeto.
     */
    Optional<TestExecution> findTopByProjectIdOrderByStartTimeDesc(UUID projectId);
}
