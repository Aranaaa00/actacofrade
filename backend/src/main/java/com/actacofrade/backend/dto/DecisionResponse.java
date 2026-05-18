package com.actacofrade.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DecisionResponse(
        Integer id,
        Integer eventId,
        String area,
        String title,
        String description,
        LocalDate deadline,
        String status,
        Integer reviewedById,
        String reviewedByName,
        boolean reviewedByVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
