package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RejectTaskRequest(

        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(max = 500, message = "El motivo de rechazo no puede superar los 500 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El texto contiene caracteres no permitidos")
        String rejectionReason
) {}
