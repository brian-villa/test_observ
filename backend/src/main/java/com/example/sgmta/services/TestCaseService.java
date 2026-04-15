package com.example.sgmta.services;

import com.example.sgmta.entities.TestCase;
import com.example.sgmta.repositories.TestCaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
     * @param testName O nome descritivo do teste (ex: "Should render login button").
     * @return A entidade TestCase persistida (recuperada ou recém-criada).
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
}
