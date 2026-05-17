package com.example.sgmta.repositories;

import com.example.sgmta.entities.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para a entidade TestCase.
 * Gere o catálogo de testes únicos do sistema.
 */

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    /**
     * Procura um caso de teste pelo seu nome descritivo.
     * Fundamental para o motor de ingestão agregar resultados de execuções
     * diferentes ao mesmo caso de teste base.
     *
     * @param testName O nome/descrição do teste (ex: "Should render login button").
     * @return Um Optional contendo o TestCase, se encontrado.
     */
    Optional<TestCase> findByTestName(String testName);

    /**
     * Procura um caso de teste pelo nome DENTRO do contexto de um projeto específico.
     * Usa EXISTS assim que encontra o primeiro resultado,
     * evitando varrer toda a tabela test_result com JOIN + DISTINCT.
     *
     * @param testName  O nome do teste.
     * @param projectId O ID do projeto.
     * @return Um Optional contendo o TestCase, se encontrado neste projeto.
     */
    @Query("SELECT tc FROM TestCase tc " +
            "WHERE tc.testName = :testName " +
            "AND EXISTS (" +
            "  SELECT 1 FROM TestResult tr " +
            "  JOIN tr.testExecution te " +
            "  WHERE tr.testCase = tc AND te.project.id = :projectId" +
            ")")
    Optional<TestCase> findByTestNameAndProjectId(
            @Param("testName") String testName,
            @Param("projectId") UUID projectId
    );

}
