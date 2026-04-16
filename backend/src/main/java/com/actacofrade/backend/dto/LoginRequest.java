package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 100, message = "La contraseña no puede superar los 100 caracteres")
        String password
) {}
