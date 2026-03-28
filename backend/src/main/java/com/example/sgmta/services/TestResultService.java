package com.example.sgmta.services;

import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestResultService {

    private final TestResultRepository testResultRepository;

    public TestResultService(TestResultRepository testResultRepository) {
        this.testResultRepository = testResultRepository;
    }

    /**
     * Cria e persiste o resultado individual de um teste.
     */
    @Transactional
    public TestResult createResult(String resultStatus, TestExecution testExecution, TestCase testCase) {

        TestResult newResult = new TestResult(resultStatus, testExecution, testCase);
        return testResultRepository.save(newResult);
    }

    public List<TestResult> findAll() {
        return testResultRepository.findAll();
    }
}