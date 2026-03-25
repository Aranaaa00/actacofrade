package com.actacofrade.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventResponse(
        Integer id,
        String reference,
        String title,
        String eventType,
        LocalDate eventDate,
        String location,
        String observations,
        String status,
        Integer responsibleId,
        String responsibleName,
        Boolean isLockedForClosing,
        long pendingTasks,
        long openIncidents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
