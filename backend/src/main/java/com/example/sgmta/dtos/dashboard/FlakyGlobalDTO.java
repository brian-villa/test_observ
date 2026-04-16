package com.example.sgmta.dtos.dashboard;

import java.util.UUID;

public record FlakyGlobalDTO(
        UUID id,
        String testCaseName,
        UUID testExecutionId
) {

}
