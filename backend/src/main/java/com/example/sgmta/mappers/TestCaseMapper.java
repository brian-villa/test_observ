package com.example.sgmta.mappers;

import com.example.sgmta.dtos.testCase.TestCaseResponseDTO;
import com.example.sgmta.entities.TestCase;

/**
 * Utilitário puro para conversão dos TestCase
 */
public class TestCaseMapper {

    /**
     * Converte a entidade TestCase num DTO de resposta.
     * * @param testCase A entidade a converter.
     * @return O DTO correspondente ou null se a entrada for nula.
     */
    public static TestCaseResponseDTO toResponseDTO(TestCase testCase) {
        if (testCase == null) {
            return null;
        }
        return new TestCaseResponseDTO(
                testCase.getId(),
                testCase.getTestName(),
                testCase.getFlaky()
        );
    }
}
