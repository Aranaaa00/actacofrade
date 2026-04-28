package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HermandadUpdateRequest(

        @NotBlank(message = "El nombre de la hermandad es obligatorio")
        @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
        @Pattern(
                regexp = "^[^<>]{3,200}$",
                message = "El nombre contiene caracteres no permitidos"
        )
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        @Pattern(
                regexp = "^[^<>]{0,500}$",
                message = "La descripción contiene caracteres no permitidos"
        )
        String descripcion,

        @Min(value = 1000, message = "El año de fundación debe ser mayor que 1000")
        @Max(value = 2100, message = "El año de fundación no puede superar 2100")
        Integer anioFundacion,

        @Size(max = 120, message = "La localidad no puede superar los 120 caracteres")
        @Pattern(
                regexp = "^[^<>]{0,120}$",
                message = "La localidad contiene caracteres no permitidos"
        )
        String localidad,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        @Pattern(
                regexp = "^[^<>]{0,200}$",
                message = "La dirección contiene caracteres no permitidos"
        )
        String direccionSede,

        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String emailContacto,

        @Pattern(
                regexp = "^$|^[+0-9 ()-]{6,20}$",
                message = "Introduce un teléfono válido"
        )
        String telefonoContacto,

        @Size(max = 200, message = "La URL no puede superar los 200 caracteres")
        @Pattern(
                regexp = "^$|^https://[^\\s<>]{1,193}$",
                message = "El sitio web debe empezar por https://"
        )
        String sitioWeb
) {}
