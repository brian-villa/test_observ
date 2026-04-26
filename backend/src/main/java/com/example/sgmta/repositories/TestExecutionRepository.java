package com.example.sgmta.repositories;

import com.example.sgmta.entities.TestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    /**
     * Busca os nomes únicos das suites executadas num projeto.
     */
    @Query("SELECT DISTINCT te.suiteName FROM TestExecution te WHERE te.project.id = :projectId AND te.suiteName IS NOT NULL")
    List<String> findDistinctSuiteNamesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Busca os nomes únicos das versões executadas num projeto.
     */
    @Query("SELECT DISTINCT v.versionName FROM TestExecution te JOIN te.version v WHERE te.project.id = :projectId")
    List<String> findDistinctVersionNamesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Conta o total de execuções de um projeto específico para a plataforma.
     */
    long countByProjectId(UUID projectId);

    /**
     * Busca a execução exata para a combinação de Projeto, Suite de Testes e ID da Execução.
     */
    Optional<TestExecution> findTopByProjectIdAndSuiteNameAndRunId(UUID projectId, String suiteName, String runId);

    /**
     * Busca as execuções de um projeto com filtros opcionais de branch e versão, devolvendo de forma paginada.
     */
    @Query(value = "SELECT te FROM TestExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName)",
            countQuery = "SELECT count(te) FROM TestExecution te " +
                    "LEFT JOIN te.version v " +
                    "WHERE te.project.id = :projectId " +
                    "AND (:branchName IS NULL OR te.branchName = :branchName) " +
                    "AND (:versionName IS NULL OR v.versionName = :versionName) " +
                    "AND (:suiteName IS NULL OR te.suiteName = :suiteName)")
    Page<TestExecution> findFilteredHistory(
            @Param("projectId") UUID projectId,
            @Param("branchName") String branchName,
            @Param("versionName") String versionName,
            @Param("suiteName") String suiteName,
            Pageable pageable
    );

    @Query("SELECT DISTINCT te.branchName FROM TestExecution te WHERE te.project.id = :projectId AND te.branchName IS NOT NULL")
    List<String> findDistinctBranchNamesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Devolve o nome da versão mais recente do projeto (pela startTime mais alta).
     */
    @Query("SELECT te.version.versionName FROM TestExecution te " +
            "WHERE te.project.id = :projectId AND te.version IS NOT NULL " +
            "ORDER BY te.startTime DESC")
    List<String> findLatestVersionNameByProjectId(@Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Busca branches disponíveis para uma versão específica.
     */
    @Query("SELECT DISTINCT te.branchName FROM TestExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND te.branchName IS NOT NULL " +
            "AND (:versionName IS NULL OR v.versionName = :versionName)")
    List<String> findDistinctBranchNamesByProjectIdAndVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName
    );

    /**
     * Busca suites disponíveis para uma versão específica.
     */
    @Query("SELECT DISTINCT te.suiteName FROM TestExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND te.suiteName IS NOT NULL " +
            "AND (:versionName IS NULL OR v.versionName = :versionName)")
    List<String> findDistinctSuiteNamesByProjectIdAndVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName
    );

}