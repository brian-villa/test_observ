package com.example.sgmta.controllers;

import com.example.sgmta.dtos.testCase.TestCaseResponseDTO;
import com.example.sgmta.entities.TestCase;
import com.example.sgmta.mappers.TestCaseMapper;
import com.example.sgmta.services.TestCaseService;
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

/**
 * Controller responsável pela exposição do catálogo de casos de teste.
 */
@RestController
@RequestMapping("/test-cases")
@Tag(name = "Casos de Teste", description = "Endpoints de leitura dos testes individuais")
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @Operation(summary = "Listar todos os casos de teste", description = "Retorna o catálogo completo de testes registados e o seu estado flaky.")
    @ApiResponse(responseCode = "200", description = "Catálogo recuperado com sucesso")
    @GetMapping
    public ResponseEntity<List<TestCaseResponseDTO>> findAll() {
        List<TestCase> testCases = testCaseService.findAll();

        List<TestCaseResponseDTO> response = testCases.stream()
                .map(TestCaseMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
