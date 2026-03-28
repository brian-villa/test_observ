package com.example.sgmta.mappers;

import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.TestResult;
import java.util.UUID;

public class TestResultMapper {

    public static TestResultResponseDTO toResponseDTO(TestResult result) {
        if (result == null) {
            return null;
        }

        // Navegação segura pelas relações ManyToOne
        String testCaseName = (result.getTestCase() != null) ? result.getTestCase().getTestName() : "Desconhecido";
        Boolean isFlaky = (result.getTestCase() != null) ? result.getTestCase().isFlaky() : false;
        UUID executionId = (result.getTestExecution() != null) ? result.getTestExecution().getId() : null;

        return new TestResultResponseDTO(
                result.getId(),
                result.getResult(),
                testCaseName,
                isFlaky,
                executionId
        );
    }
}