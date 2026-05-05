package com.actacofrade.backend.dto;

import java.time.OffsetDateTime;

/** Public view of an admin change request. */
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
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt
) {}
