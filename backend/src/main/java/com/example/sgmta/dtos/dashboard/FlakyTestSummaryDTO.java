package com.example.sgmta.dtos.dashboard;

public record FlakyTestSummaryDTO(
        Long id,
        String name,
        String failureRate
) {}