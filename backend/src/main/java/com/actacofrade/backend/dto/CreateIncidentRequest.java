package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(

        @NotBlank(message = "La descripcion de la incidencia es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        Integer reportedById
) {}
