package com.example.sgmta.services;

import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.repositories.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TestResultService {

    private final TestResultRepository testResultRepository;

    public TestResultService(TestResultRepository testResultRepository) {
        this.testResultRepository = testResultRepository;
    }

    @Transactional
    public TestResult createResult(String resultStatus, String errorMessage, TestExecution testExecution, TestCase testCase) {
        TestResult newResult = new TestResult(resultStatus, false, errorMessage, testExecution, testCase);
        return testResultRepository.save(newResult);
    }

    @Transactional
    public TestResult save(TestResult result) {
        return testResultRepository.save(result);
    }

    public long countFailures(TestCase testCase, Project project) {
        return testResultRepository.countByTestCaseAndTestExecution_ProjectAndResult(testCase, project, "FAIL");
    }

    public List<TestResult> findAll() {
        return testResultRepository.findAll();
    }

    public List<TestResult> findByExecutionId(UUID executionId) {
        return testResultRepository.findByTestExecutionId(executionId);
    }

    public Page<TestResult> findFilteredByExecutionId(UUID executionId, String searchTerm, String status, boolean flakyOnly, Pageable pageable) {
        String searchParam = (searchTerm == null || searchTerm.trim().isEmpty()) ? "" : searchTerm.trim();
        Boolean flakyParam = flakyOnly ? true : null;

        return testResultRepository.findFilteredResults(executionId, searchParam, status, flakyParam, pageable);
    }
}