package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String fullName,

        @Email(message = "Introduce un correo electrónico válido")
        String email,

        @Pattern(regexp = "ADMINISTRADOR|RESPONSABLE|COLABORADOR|CONSULTA", message = "Rol no válido")
        String roleCode
) {}
