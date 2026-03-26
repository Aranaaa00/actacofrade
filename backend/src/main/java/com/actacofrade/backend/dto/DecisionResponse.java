package com.actacofrade.backend.dto;

import java.time.LocalDateTime;

public record DecisionResponse(
        Integer id,
        Integer eventId,
        String area,
        String title,
        String status,
        Integer reviewedById,
        String reviewedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
