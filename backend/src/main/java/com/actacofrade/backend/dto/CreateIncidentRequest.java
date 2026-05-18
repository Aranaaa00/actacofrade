package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateIncidentRequest(

        @NotBlank(message = "La descripcion de la incidencia es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        @NotNull(message = "Debes asignar a un responsable de la incidencia")
        Integer reportedById,

        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Las notas contienen caracteres no permitidos")
        String notes,

        @NotNull(message = "La fecha limite es obligatoria")
        LocalDate deadline
) {}
