package com.example.sgmta.mappers;

import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.TestResult;
import java.util.UUID;

/**
 * Utilitário puro para conversão dos TestResult
 */

public class TestResultMapper {

    public static TestResultResponseDTO toResponseDTO(TestResult result) {

        String testCaseName = (result.getTestCase() != null) ? result.getTestCase().getTestName() : "Desconhecido";
        UUID executionId = (result.getTestExecution() != null) ? result.getTestExecution().getId() : null;

        boolean isFlaky = (result.getFlaky() != null) ? result.getFlaky() : false;

        String reason = determineFlakyReason(isFlaky);

        return new TestResultResponseDTO(
                result.getId(),
                result.getResult(),
                testCaseName,
                isFlaky,
                executionId,
                reason,
                result.getErrorMessage(),
                result.getScreenshot()
        );
    }

    /**
     * Motor de decisão para as mensagens de Flaky.
     * Facilita a expansão futura para diferentes cenários de instabilidade.
     */
    private static String determineFlakyReason(boolean isFlakyDbFlag) {
        if (!isFlakyDbFlag) {
            return null;
        }

        // TODO: Adicionar lógica para detetar Flip-Flop


        // TODO: Adicionar verificação de flag manual
        return "Este teste falhou consecutivamente em execuções recentes excedendo o limiar do projeto e demonstrou instabilidade ao alternar resultados.";
    }
}