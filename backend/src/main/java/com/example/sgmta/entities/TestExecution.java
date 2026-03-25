package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_execution")
@Getter
@Setter
@Schema(description = "Representa uma única execução de um suite de teste de CI/CD pipeline.")
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Identificador único do TestExecution.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Column(name = "execution_date", nullable = false)
    @Schema(description = "A data e o horário em que a execução foi iniciada.", example = "2026-03-17T10:00:00")
    private LocalDateTime executionDate;

    @Column(name = "branch_name")
    @Schema(description = "O nome da branch que o teste está a executar.", example = "feature/new-login")
    private String branchName;

    @Column(name = "start_time")
    @Schema(description = "O timestamp do início da execução do teste.", example = "2026-03-17T09:55:00")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @Schema(description = "O timestamp do fim da execução do teste.", example = "2026-03-17T10:05:00")
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Schema(description = "O projeto que essa suite de teste está associada")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @Schema(description = "A versão do software que está a executar o teste.")
    private Version version;

    protected TestExecution() {}

    public TestExecution(UUID id, LocalDateTime executionDate, String branchName, LocalDateTime startTime, LocalDateTime endTime, Project project, Version version) {
        this.id = id;
        this.executionDate = executionDate;
        this.branchName = branchName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.project = project;
        this.version = version;
    }

}
