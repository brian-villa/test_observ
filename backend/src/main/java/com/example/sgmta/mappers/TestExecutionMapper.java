package com.example.sgmta.mappers;

import com.example.sgmta.dtos.testExecution.TestExecutionResponseDTO;
import com.example.sgmta.entities.TestExecution;

public class TestExecutionMapper {

    public static TestExecutionResponseDTO toResponseDTO(TestExecution execution) {
        if (execution == null) {
            return null;
        }

        String projectName = (execution.getProject() != null) ? execution.getProject().getName() : "Desconhecido";
        String versionName = (execution.getVersion() != null) ? execution.getVersion().getVersionName() : "Desconhecida";

        return new TestExecutionResponseDTO(
                execution.getId(),
                projectName,
                versionName,
                execution.getBranchName(),
                execution.getSuiteName(),
                execution.getRunId(),
                execution.getExecutionDate(),
                execution.getStartTime(),
                execution.getEndTime()
        );
    }
}