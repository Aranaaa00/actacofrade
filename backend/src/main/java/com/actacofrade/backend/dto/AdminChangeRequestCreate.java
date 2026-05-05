package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChangeRequestCreate(
        @NotBlank(message = "El mensaje es obligatorio")
        @Size(min = 10, max = 2000, message = "El mensaje debe tener entre 10 y 2000 caracteres")
        String message
) {}
