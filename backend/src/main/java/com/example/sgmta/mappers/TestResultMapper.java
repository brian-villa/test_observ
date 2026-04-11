package com.example.sgmta.mappers;

import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.TestResult;
import java.util.UUID;

public class TestResultMapper {

    public static TestResultResponseDTO toResponseDTO(TestResult result) {

        String testCaseName = (result.getTestCase() != null) ? result.getTestCase().getTestName() : "Desconhecido";
        UUID executionId = (result.getTestExecution() != null) ? result.getTestExecution().getId() : null;

        boolean isFlaky = false;
        if (result.getTestCase() != null && result.getTestCase().getFlaky() != null) {
            isFlaky = result.getTestCase().getFlaky();
        }

        String reason = determineFlakyReason(isFlaky, result);

        return new TestResultResponseDTO(
                result.getId(),
                result.getResult(),
                testCaseName,
                isFlaky,
                executionId,
                reason
        );
    }

    /**
     * Motor de decisão para as mensagens de Flaky.
     * Facilita a expansão futura para diferentes cenários de instabilidade.
     */
    private static String determineFlakyReason(boolean isFlakyDbFlag, TestResult currentResult) {
        if (!isFlakyDbFlag) {
            return null;
        }

        // TODO FUTURO: Adicionar aqui a lógica para detetar Flip-Flop (Cenário B)


        // TODO FUTURO: Adicionar verificação de flag manual (Cenário C)

        return "Histórico Global: Este teste falhou consecutivamente em execuções recentes (Threshold excedido). Está sinalizado como instável e em quarentena.";
    }
}