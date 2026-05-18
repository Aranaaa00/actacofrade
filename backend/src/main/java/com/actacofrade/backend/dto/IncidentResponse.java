package com.actacofrade.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentResponse(
        Integer id,
        Integer eventId,
        String description,
        String notes,
        LocalDate deadline,
        String status,
        Integer reportedById,
        String reportedByName,
        boolean reportedByVerified,
        Integer resolvedById,
        String resolvedByName,
        boolean resolvedByVerified,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
