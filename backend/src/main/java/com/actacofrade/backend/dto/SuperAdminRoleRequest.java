package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SuperAdminRoleRequest(
        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "ADMINISTRADOR|RESPONSABLE|COLABORADOR|CONSULTA",
                message = "Rol no válido. SUPER_ADMIN no se puede asignar manualmente.")
        String roleCode,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String reason
) {
}
