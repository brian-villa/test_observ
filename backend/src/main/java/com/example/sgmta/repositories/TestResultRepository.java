package com.example.sgmta.repositories;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    /**
     * Conta o número de falhas de um Caso de Teste específico dentro de um Projeto.
     * Navega: TestResult -> TestExecution -> Project
     */
    long countByTestCaseAndTestExecution_ProjectAndResult(TestCase testCase, Project project, String result);
}
