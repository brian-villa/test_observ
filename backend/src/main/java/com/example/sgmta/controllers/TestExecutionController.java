package com.example.sgmta.controllers;

import com.example.sgmta.dtos.testExecution.TestExecutionResponseDTO;
import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.mappers.TestExecutionMapper;
import com.example.sgmta.mappers.TestResultMapper;
import com.example.sgmta.services.TestExecutionService;
import com.example.sgmta.services.TestResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/executions")
@Tag(name = "Execuções de Teste", description = "Endpoints para consulta do histórico de execuções de pipelines")
@SecurityRequirement(name = "bearerAuth")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;
    private final TestResultService testResultService;

    public TestExecutionController(TestExecutionService testExecutionService, TestResultService testResultService) {
        this.testExecutionService = testExecutionService;
        this.testResultService = testResultService;
    }

    @Operation(summary = "Listar todas as execuções", description = "Retorna o histórico completo de pipelines executados em todos os projetos.")
    @ApiResponse(responseCode = "200", description = "Histórico recuperado com sucesso")
    @GetMapping
    public ResponseEntity<List<TestExecutionResponseDTO>> findAll() {
        List<TestExecution> executions = testExecutionService.findAll();

        List<TestExecutionResponseDTO> response = executions.stream()
                .map(TestExecutionMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resultados detalhados da Execução", description = "Lista paginada e filtrada de testes.")
    @ApiResponse(responseCode = "200", description = "Resultados recuperados com sucesso")
    @GetMapping("/{executionId}/results")
    public ResponseEntity<Page<TestResultResponseDTO>> getExecutionResults(
            @PathVariable UUID executionId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "false") boolean flakyOnly,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<TestResult> resultsPage = testResultService.findFilteredByExecutionId(executionId, search, status, flakyOnly, pageable);

        Page<TestResultResponseDTO> response = resultsPage.map(TestResultMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }
}