package com.actacofrade.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Integer id,
        Integer eventId,
        String title,
        String description,
        Integer assignedToId,
        String assignedToName,
        boolean assignedToVerified,
        Integer createdByUserId,
        String status,
        LocalDate deadline,
        String rejectionReason,
        Integer confirmedById,
        String confirmedByName,
        boolean confirmedByVerified,
        LocalDateTime confirmedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
