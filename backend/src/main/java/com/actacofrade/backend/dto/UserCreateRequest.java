package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        @Pattern(
                regexp = "^[\\p{L}\\p{M} .'-]{3,150}$",
                message = "El nombre solo puede contener letras, espacios y caracteres básicos de nombre propio"
        )
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        @Pattern(
                regexp = "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\d)(?=.*[@$!%*?&.#_\\-])[\\p{L}\\d@$!%*?&.#_\\-]{8,}$",
                message = "Debe incluir mayúscula, minúscula, número y carácter especial"
        )
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(
                regexp = "RESPONSABLE|COLABORADOR|CONSULTA",
                message = "Rol no válido. Solo se permiten RESPONSABLE, COLABORADOR o CONSULTA"
        )
        String roleCode
) {}
