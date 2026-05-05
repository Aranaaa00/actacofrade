package com.actacofrade.backend.dto;

import java.time.LocalDateTime;

public record AdminChangeRequestResponse(
        Integer id,
        Integer hermandadId,
        String hermandadNombre,
        Integer requesterUserId,
        String requesterFullName,
        String requesterEmail,
        String message,
        String status,
        Integer newAdminUserId,
        String newAdminFullName,
        Integer resolvedByUserId,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {}
