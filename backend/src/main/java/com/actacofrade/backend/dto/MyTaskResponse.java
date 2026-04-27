package com.actacofrade.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyTaskResponse(
        Integer id,
        Integer eventId,
        String eventType,
        String eventTitle,
        String title,
        String status,
        LocalDate deadline,
        String rejectionReason,
        LocalDateTime confirmedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {}
