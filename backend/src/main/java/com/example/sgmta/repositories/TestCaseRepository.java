package com.example.sgmta.repositories;

import com.example.sgmta.entities.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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


}
