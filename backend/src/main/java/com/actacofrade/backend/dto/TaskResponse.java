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
        String status,
        LocalDate deadline,
        String rejectionReason,
        Integer confirmedById,
        String confirmedByName,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
