package com.actacofrade.backend.dto;

import java.time.LocalDateTime;

public record InterventionLogEntry(
        Integer id,
        String action,
        Integer targetUserId,
        Integer actorId,
        String actorName,
        LocalDateTime performedAt,
        String details,
        String changes
) {
}
