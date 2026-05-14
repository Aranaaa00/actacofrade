package com.actacofrade.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SuperAdminUserResponse(
        Integer id,
        String fullName,
        String email,
        List<String> roles,
        String status,
        String statusReason,
        LocalDateTime statusChangedAt,
        boolean manuallyVerified,
        LocalDateTime manuallyVerifiedAt,
        String hermandadNombre,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
}
