package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        @Pattern(
                regexp = "^[\\p{L}\\p{M} .'-]{3,150}$",
                message = "El nombre solo puede contener letras, espacios y caracteres básicos de nombre propio"
        )
        String fullName,

        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String email,

        @Pattern(regexp = "ADMINISTRADOR|RESPONSABLE|COLABORADOR|CONSULTA", message = "Rol no válido")
        String roleCode
) {}
