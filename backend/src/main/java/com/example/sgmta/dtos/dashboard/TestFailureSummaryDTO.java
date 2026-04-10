package com.example.sgmta.dtos.dashboard;

import java.util.UUID;

public record TestFailureSummaryDTO(
        UUID id,
        String name,
        String status
) {}
