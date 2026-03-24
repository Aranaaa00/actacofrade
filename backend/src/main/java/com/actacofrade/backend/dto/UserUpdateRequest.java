package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(max = 150)
        String fullName,

        @Email
        String email,

        String roleCode
) {}
