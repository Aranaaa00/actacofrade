package com.actacofrade.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Integer id,
        String fullName,
        String email,
        List<String> roles,
        Boolean active,
        LocalDateTime lastLogin,
        boolean hasAvatar
) {}
