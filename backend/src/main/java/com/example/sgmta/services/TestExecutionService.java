package com.example.sgmta.services;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.Version;
import com.example.sgmta.repositories.TestExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestExecutionService {

    private final TestExecutionRepository testExecutionRepository;

    public TestExecutionService(TestExecutionRepository testExecutionRepository) {
        this.testExecutionRepository = testExecutionRepository;
    }

    /**
     * Cria e persiste uma nova execução de testes associada a um Projeto e Versão.
     */
    @Transactional
    public TestExecution createExecution(Project project, Version version, String branchName, LocalDateTime startTime, LocalDateTime endTime, String suiteName, String runId) {

        TestExecution newExecution = new TestExecution(
                LocalDateTime.now(),
                branchName,
                startTime,
                endTime,
                suiteName,
                runId,
                project,
                version
        );

        return testExecutionRepository.save(newExecution);
    }

    /**
     * Recupera o histórico completo de execuções.
     */
    public List<TestExecution> findAll() {
        return testExecutionRepository.findAll();
    }
}