package com.example.sgmta.controllers;

import com.example.sgmta.adapters.ReportAdapter;
import com.example.sgmta.dtos.ingestion.StandardizedPipelineReport;
import com.example.sgmta.services.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingest")
@Tag(name = "Ingestão", description = "Endpoint massivo para recebimento de resultados de pipelines CI/CD")
public class IngestionController {

    private final IngestionService ingestionService;
    private final List<ReportAdapter> adapters;

    public IngestionController(IngestionService ingestionService, List<ReportAdapter> adapters) {
        this.ingestionService = ingestionService;
        this.adapters = adapters;
    }

    @Operation(summary = "Ingerir relatório de testes", description = "Recebe XML (JUnit) ou JSON e processa a persistência de todos os resultados.")
    @PostMapping(consumes = {"application/json", "application/xml"})
    public ResponseEntity<String> ingest(
            @RequestBody String rawPayload,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("X-Project-Token") String token,
            @RequestHeader("X-Version-Name") String version,
            @RequestHeader("X-Branch-Name") String branch,
            @Parameter(description = "Nome do grupo de testes") @RequestHeader("X-Suite-Name") String suiteName,
            @Parameter(description = "ID único da pipeline") @RequestHeader("X-Execution-Id") String executionId
    ) {

        ReportAdapter adapter = adapters.stream()
                .filter(a -> a.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Formato de conteúdo não suportado: " + contentType));

        StandardizedPipelineReport report = adapter.adapt(rawPayload, token, version, branch);

        ingestionService.ingest(report, suiteName, executionId);

        return ResponseEntity.accepted().body("Relatório recebido e processado com sucesso.");
    }
}