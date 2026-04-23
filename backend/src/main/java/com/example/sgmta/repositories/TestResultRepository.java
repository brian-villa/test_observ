package com.example.sgmta.repositories;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    /**
     * Conta o número de falhas de um Caso de Teste específico dentro de um Projeto.
     * Navega: TestResult -> TestExecution -> Project
     */
    long countByTestCaseAndTestExecution_ProjectAndResult(TestCase testCase, Project project, String result);

    /**
     * Conta os resultados (PASS/FAIL) pela Execução e pelo Projeto.
     */
    long countByTestExecutionIdAndResult(UUID testExecutionId, String result);

    /**
     * Traz todos os resultados associados a uma Execução específica.
     */
    List<TestResult> findByTestExecutionId(UUID testExecutionId);

    List<TestResult> findByTestExecutionIdAndResult(UUID testExecutionId, String result);

    /**
     * Verifica de forma rápida se existe pelo menos um resultado com um estado específico ("FAIL")
     * associado a uma determinada execução.
     */
    boolean existsByTestExecutionIdAndResult(UUID testExecutionId, String result);

    @Query(value = "SELECT tr FROM TestResult tr " +
            "WHERE tr.testExecution.id = :executionId " +
            "AND (:searchTerm IS NULL OR LOWER(tr.testCase.testName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:status = 'ALL' OR tr.result = :status) " +
            "AND (:isFlaky IS NULL OR tr.flaky = :isFlaky)",
            countQuery = "SELECT count(tr) FROM TestResult tr " +
                    "WHERE tr.testExecution.id = :executionId " +
                    "AND (:searchTerm IS NULL OR LOWER(tr.testCase.testName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                    "AND (:status = 'ALL' OR tr.result = :status) " +
                    "AND (:isFlaky IS NULL OR tr.flaky = :isFlaky)")
    Page<TestResult> findFilteredResults(
            @Param("executionId") UUID executionId,
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("isFlaky") Boolean isFlaky,
            Pageable pageable);

    /**
     * Busca APENAS os testes que estão ATIVAMENTE instáveis.
     * Retorna o resultado se ele for 'flaky=true' E for a execução mais recente daquele TestCase.
     */
    @Query("SELECT r FROM TestResult r " +
            "WHERE r.testExecution.project.id = :projectId " +
            "AND r.flaky = true " +
            "AND r.testExecution.startTime = (" +
            "   SELECT MAX(r2.testExecution.startTime) " +
            "   FROM TestResult r2 " +
            "   WHERE r2.testCase.id = r.testCase.id " +
            "   AND r2.testExecution.project.id = :projectId" +
            ")")
    List<TestResult> findActiveFlakyTestsByProjectId(@Param("projectId") UUID projectId);

    long countByTestExecutionIdAndFlakyTrue(UUID testExecutionId);

    List<TestResult> findByTestExecutionIdAndFlakyTrue(UUID testExecutionId);

    @Query("SELECT r FROM TestResult r WHERE r.testCase.id = :testCaseId AND r.testExecution.project.id = :projectId")
    Page<TestResult> findRecentResultsByTestCaseAndProject(
            @Param("testCaseId") UUID testCaseId,
            @Param("projectId") UUID projectId,
            Pageable pageable);


}
