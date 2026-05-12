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
            Pageable pageable
    );

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

    /**
     * Conta Flakys dentro de uma execução garantindo que não conta o mesmo teste duas vezes (ex: Retries)
     */
    @Query("SELECT COUNT(DISTINCT r.testCase.id) FROM TestResult r WHERE r.testExecution.id = :testExecutionId AND r.flaky = true")
    long countDistinctFlakyByTestExecutionId(@Param("testExecutionId") UUID testExecutionId);

    List<TestResult> findByTestExecutionIdAndFlakyTrue(UUID testExecutionId);


    @Query("SELECT r FROM TestResult r WHERE r.testCase.id = :testCaseId AND r.testExecution.project.id = :projectId AND r.testExecution.version.id = :versionId")
    Page<TestResult> findRecentResultsByTestCaseProjectAndVersion(
            @Param("testCaseId") UUID testCaseId,
            @Param("projectId") UUID projectId,
            @Param("versionId") UUID versionId,
            Pageable pageable
    );

    @Query("SELECT r FROM TestResult r WHERE r.testCase.id = :testCaseId AND r.testExecution.project.id = :projectId")
    Page<TestResult> findRecentResultsByTestCaseAndProject(
            @Param("testCaseId") UUID testCaseId,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );


    /**
     * Pesquisa Global: Procura testes pelo nome dentro de um projeto
     * e retorna APENAS a "fotografia" mais recente desse teste.
     */
    @Query("SELECT r FROM TestResult r " +
            "LEFT JOIN r.testExecution.version v " +
            "WHERE r.testExecution.project.id = :projectId " +
            "AND (:branchName IS NULL OR r.testExecution.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR r.testExecution.suiteName = :suiteName) " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND LOWER(r.testCase.testName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "AND r.testExecution.startTime = (" +
            "   SELECT MAX(r2.testExecution.startTime) " +
            "   FROM TestResult r2 " +
            "   LEFT JOIN r2.testExecution.version v2 " +
            "   WHERE r2.testCase.id = r.testCase.id " +
            "   AND r2.testExecution.project.id = :projectId " +
            "   AND (:branchName IS NULL OR r2.testExecution.branchName = :branchName) " +
            "   AND (:suiteName IS NULL OR r2.testExecution.suiteName = :suiteName)" +
            "   AND (:versionName IS NULL OR v2.versionName = :versionName)" +
            ")")
    List<TestResult> searchLatestResultsByTestName(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm,
            @Param("branchName") String branchName,
            @Param("suiteName") String suiteName,
            @Param("versionName") String versionName,
            Pageable pageable
    );

    /**
     * Agrega os resultados de testes por suite dentro de uma versão específica.
     * Devolve: [suiteName, totalPassed, totalFailed, buildCount]
     * Usado para construir a Pirâmide de Testes no dashboard.
     */
    @Query("SELECT te.suiteName, " +
            "SUM(CASE WHEN r.result = 'PASS' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.result = 'FAIL' THEN 1 ELSE 0 END), " +
            "COUNT(DISTINCT te.buildName) " +
            "FROM TestResult r " +
            "JOIN r.testExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName) " +
            "GROUP BY te.suiteName " +
            "ORDER BY te.suiteName")
    List<Object[]> aggregateResultsBySuiteAndVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName,
            @Param("branchName") String branchName,
            @Param("suiteName") String suiteName
    );

    /**
     * Conta o total de testes (PASS + FAIL) de todas as builds dentro de uma versão, por suite.
     * Usado para calcular o health score da versão.
     */
    @Query("SELECT SUM(CASE WHEN r.result = 'PASS' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.result = 'FAIL' THEN 1 ELSE 0 END) " +
            "FROM TestResult r " +
            "JOIN r.testExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName)")
    List<Object[]> sumPassFailByVersion(
                                         @Param("projectId") UUID projectId,
                                         @Param("versionName") String versionName,
                                         @Param("branchName") String branchName,
                                         @Param("suiteName") String suiteName
    );

    /**
     * Busca testes flaky activos dentro de uma versão específica.
     */
    @Query("SELECT r FROM TestResult r " +
            "JOIN r.testExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND r.flaky = true " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName) " +
            "AND te.startTime = (" +
            "   SELECT MAX(r2.testExecution.startTime) " +
            "   FROM TestResult r2 " +
            "   LEFT JOIN r2.testExecution.version v2 " +
            "   WHERE r2.testCase.id = r.testCase.id " +
            "   AND r2.testExecution.project.id = :projectId " +
            "   AND (:versionName IS NULL OR v2.versionName = :versionName)" +
            ")")
    List<TestResult> findActiveFlakyTestsByVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName,
            @Param("branchName") String branchName,
            @Param("suiteName") String suiteName
    );

    /**
     * Conta flakys activos numa versão.
     */
    @Query("SELECT COUNT(DISTINCT r.testCase.id) FROM TestResult r " +
            "JOIN r.testExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND r.flaky = true " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName)")
    long countActiveFlakyByVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName,
            @Param("branchName") String branchName,
            @Param("suiteName") String suiteName
    );

    /**
     * Busca falhas críticas (FAIL) dentro de uma versão, da build mais recente com falhas.
     */
    @Query("SELECT r FROM TestResult r " +
            "JOIN r.testExecution te " +
            "LEFT JOIN te.version v " +
            "WHERE te.project.id = :projectId " +
            "AND r.result = 'FAIL' " +
            "AND (:versionName IS NULL OR v.versionName = :versionName) " +
            "AND (:branchName IS NULL OR te.branchName = :branchName) " +
            "AND (:suiteName IS NULL OR te.suiteName = :suiteName) " +
            "ORDER BY te.startTime DESC")
    List<TestResult> findRecentFailuresByVersion(
            @Param("projectId") UUID projectId,
            @Param("versionName") String versionName,
            @Param("branchName") String branchName,
            @Param("suiteName") String suiteName,
            Pageable pageable
    );

}
