package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Introduce un correo electrónico válido")
        String email
) {}
