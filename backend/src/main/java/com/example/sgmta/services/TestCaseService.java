package com.example.sgmta.services;

import com.example.sgmta.entities.TestCase;
import com.example.sgmta.repositories.TestCaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pela gestão do catálogo de testes individuais.
 * Garante que cada teste único é registado apenas uma vez,
 * permitindo rastrear casos flaky.
 */

@Service
public class TestCaseService {
    private final TestCaseRepository testCaseRepository;

    public TestCaseService(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Lógica de "Find or Create" para os casos de teste.
     * Interage com o repositório para evitar duplicação de testes com o mesmo nome.
     *
     * @param testName O nome descritivo do teste.
     * @return A entidade TestCase armazenada.
     */

    @Transactional
    public TestCase findOrCreate(String testName) {
        Optional<TestCase> existingTest = testCaseRepository.findByTestName(testName);

        if (existingTest.isPresent()) {
            return existingTest.get();
        }

        TestCase newTest = new TestCase(testName);

        return testCaseRepository.save(newTest);
    }

    /**
     * Lógica de "Find or Create" ligado ao projeto.
     * Garante que dois projetos com testes de mesmo nome ficam com TestCase distintos,
     * preservando a integridade do histórico  por projeto.
     *
     * @param testName  O nome descritivo do teste.
     * @param projectId O ID do projeto ao qual a ingestão pertence.
     * @return A entidade TestCase armazenada, única dentro do contexto deste projeto.
     */
    @Transactional
    public TestCase findOrCreate(String testName, UUID projectId) {
        //Procurar um TestCase já usado neste projeto
        Optional<TestCase> existingTest = testCaseRepository.findByTestNameAndProjectId(testName, projectId);

        if (existingTest.isPresent()) {
            return existingTest.get();
        }

        // Não existe, criar um novo TestCase
        TestCase newTest = new TestCase(testName);
        return testCaseRepository.save(newTest);
    }
}
