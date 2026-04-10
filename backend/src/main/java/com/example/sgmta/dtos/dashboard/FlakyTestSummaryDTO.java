package com.example.sgmta.dtos.dashboard;

import java.util.UUID;

public record FlakyTestSummaryDTO(
        UUID id,
        String name,
        String failureRate
) {}