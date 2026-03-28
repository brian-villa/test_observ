package com.example.sgmta.controllers;

import com.example.sgmta.dtos.testExecution.TestExecutionResponseDTO;
import com.example.sgmta.entities.TestExecution;
import com.example.sgmta.mappers.TestExecutionMapper;
import com.example.sgmta.services.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/executions")
@Tag(name = "Execuções de Teste", description = "Endpoints para consulta do histórico de execuções de pipelines")
@SecurityRequirement(name = "bearerAuth")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    public TestExecutionController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
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
}