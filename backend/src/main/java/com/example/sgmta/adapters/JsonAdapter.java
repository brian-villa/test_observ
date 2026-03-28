package com.example.sgmta.adapters;

import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonAdapter implements ReportAdapter {

    private final ObjectMapper jsonMapper;

    public JsonAdapter() {
        this.jsonMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    @Override
    public StandardizedPipelineReport adapt(String rawPayload, String projectToken, String versionName, String branchName) {
        try {
            // Converte o JSON bruto para a nossa estrutura de entrada
            JsonPayload inputData = jsonMapper.readValue(rawPayload, JsonPayload.class);

            List<StandardizedPipelineReport.TestCaseResult> standardizedTests = new ArrayList<>();

            if (inputData.tests() != null) {
                for (JsonTestCase test : inputData.tests()) {
                    standardizedTests.add(new StandardizedPipelineReport.TestCaseResult(
                            test.name(),
                            test.status().toUpperCase(),
                            test.durationMs()
                    ));
                }
            }

            return new StandardizedPipelineReport(
                    projectToken,
                    versionName,
                    branchName,
                    inputData.startTime() != null ? inputData.startTime() : LocalDateTime.now().minusMinutes(5),
                    inputData.endTime() != null ? inputData.endTime() : LocalDateTime.now(),
                    standardizedTests
            );

        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o formato JSON: " + e.getMessage(), e);
        }
    }

    @Schema(description = "Estrutura esperada quando o payload de ingestão é enviado em formato JSON")
    public record JsonPayload(
            @Schema(description = "Timestamp opcional de início", example = "2026-03-17T09:55:00")
            LocalDateTime startTime,

            @Schema(description = "Timestamp opcional de fim", example = "2026-03-17T10:05:00")
            LocalDateTime endTime,

            @Schema(description = "Array com os resultados dos testes")
            List<JsonTestCase> tests
    ) {}

    @Schema(description = "Representação de um caso de teste no payload JSON")
    public record JsonTestCase(
            @Schema(description = "Nome do teste", example = "loginWithSuccess")
            String name,

            @Schema(description = "Status do teste (ex: PASS, FAIL)", example = "PASS")
            String status,

            @Schema(description = "Duração em milissegundos", example = "150")
            Long durationMs
    ) {}
}