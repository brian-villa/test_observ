package com.example.sgmta.controllers;

import com.example.sgmta.dtos.testResult.TestResultResponseDTO;
import com.example.sgmta.entities.TestResult;
import com.example.sgmta.mappers.TestResultMapper;
import com.example.sgmta.services.TestResultService;
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
@RequestMapping("/results")
@Tag(name = "Resultados de Testes", description = "Endpoints para consulta dos resultados individuais")
@SecurityRequirement(name = "bearerAuth")
public class TestResultController {

    private final TestResultService testResultService;

    public TestResultController(TestResultService testResultService) {
        this.testResultService = testResultService;
    }

    @Operation(summary = "Listar todos os resultados", description = "Retorna o histórico de todos os testes executados.")
    @ApiResponse(responseCode = "200", description = "Resultados recuperados com sucesso")
    @GetMapping
    public ResponseEntity<List<TestResultResponseDTO>> findAll() {
        List<TestResult> results = testResultService.findAll();

        List<TestResultResponseDTO> response = results.stream()
                .map(TestResultMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}