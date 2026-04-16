package com.actacofrade.backend.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Integer id,
        Integer eventId,
        String entityType,
        Integer entityId,
        String action,
        Integer performedById,
        String performedByName,
        LocalDateTime performedAt,
        String details
) {}
