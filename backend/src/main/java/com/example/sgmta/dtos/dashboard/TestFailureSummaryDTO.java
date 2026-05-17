package com.example.sgmta.dtos.dashboard;

import java.util.UUID;
import com.example.sgmta.entities.enums.TestStatus;

public record TestFailureSummaryDTO(
        UUID id,
        String name,
        TestStatus status
) {}
