package com.example.sgmta.dtos.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agregação de resultados de testes por suite, acumulada de todas as builds de uma versão")
public record SuiteAggregationDTO(

        @Schema(description = "Nome da suite de testes", example = "E2E")
        String suiteName,

        @Schema(description = "Total de testes que passaram em todas as builds desta versão", example = "450")
        long totalPassed,

        @Schema(description = "Total de testes que falharam em todas as builds desta versão", example = "12")
        long totalFailed,

        @Schema(description = "Total de testes executados (passed + failed)", example = "462")
        long totalTests,

        @Schema(description = "Número de builds distintas que executaram esta suite nesta versão", example = "5")
        long buildCount
) {}
