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
            @RequestHeader("Project-Token") String token,
            @RequestHeader("Version-Name") String version,
            @RequestHeader("Branch-Name") String branch,
            @Parameter(description = "Nome do grupo de testes") @RequestHeader("Suite-Name") String suiteName,
            @Parameter(description = "ID único da pipeline") @RequestHeader("Execution-Id") String executionId,
            @Parameter(description = "Nome amigável da build") @RequestHeader(value = "Build-Name", required = false, defaultValue = "Build Desconhecida") String buildName
    ) {

        ReportAdapter adapter = adapters.stream()
                .filter(a -> a.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Formato de conteúdo não suportado: " + contentType));

        StandardizedPipelineReport report = adapter.adapt(rawPayload, token, version, branch);

        ingestionService.ingest(report, suiteName, executionId, buildName);

        return ResponseEntity.accepted().body("Relatório recebido e processado com sucesso.");
    }
}