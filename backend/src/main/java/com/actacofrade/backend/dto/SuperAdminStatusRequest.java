package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de cambio de estado de cuenta emitida por el SuperAdmin.
 * El motivo es obligatorio cuando el nuevo estado no es {@code ACTIVE}; esa
 * regla se valida adicionalmente a nivel de servicio.
 */
public record SuperAdminStatusRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "ACTIVE|SUSPENDED|BANNED",
                message = "Estado no válido. Permitidos: ACTIVE, SUSPENDED, BANNED.")
        String status,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String reason
) {
}
