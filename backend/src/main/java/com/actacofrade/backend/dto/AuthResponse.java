package com.actacofrade.backend.dto;

import java.util.List;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        List<String> roles,
        String hermandadNombre
) {}
