package com.actacofrade.backend.dto;

public record MyTaskStatsResponse(
        long pendingCount,
        long confirmedCount,
        long rejectedCount
) {}
