package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "test_result")
@Getter
@Setter
@Schema(description = "Representa o resultado de um único caso de teste.")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Identificador único do resultado", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Column(name = "result", nullable = false)
    @Schema(description = "O resultado do teste.", example = "PASS")
    private String result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", nullable = false)
    @Schema(description = "A execução desse teste pertence a que suite de teste")
    private TestExecution testExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @Schema(description = "O TestCase que esse resulto pertence")
    private TestCase testCase;

    protected TestResult() {}

    public TestResult(UUID id, String result, TestExecution testExecution, TestCase testCase) {
        this.id = id;
        this.result = result;
        this.testExecution = testExecution;
        this.testCase = testCase;
    }
}
