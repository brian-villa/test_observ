package com.example.sgmta.repositories;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestResult;
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
    // Conta quantos testes passaram/falharam numa execução específica
    long countByTestExecutionIdAndResult(UUID testExecutionId, String result);

    // Traz a lista de testes que passaram/falharam numa execução específica
    List<TestResult> findByTestExecutionIdAndResult(UUID testExecutionId, String result);

    /**
     * Verifica de forma rápida se existe pelo menos um resultado com um estado específico ("FAIL")
     * associado a uma determinada execução.
     */
    boolean existsByTestExecutionIdAndResult(UUID testExecutionId, String result);

    /**
     * Conta os casos de teste únicos que estão marcados como Flaky
     * e que pertencem ao histórico deste projeto específico.
     */
    @Query("SELECT COUNT(DISTINCT tr.testCase) FROM TestResult tr WHERE tr.testExecution.project.id = :projectId AND tr.testCase.flaky = true")
    long countFlakyTestsByProjectId(@Param("projectId") UUID projectId);


}
