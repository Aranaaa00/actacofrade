package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateDecisionRequest(

        @NotBlank(message = "El area de la decision es obligatoria")
        @Pattern(regexp = "MAYORDOMIA|SECRETARIA|PRIOSTIA|TESORERIA|DIPUTACION_MAYOR", message = "\u00c1rea no v\u00e1lida")
        String area,

        @NotBlank(message = "El titulo de la decision es obligatorio")
        @Size(max = 255, message = "El titulo de la decision no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El título contiene caracteres no permitidos")
        String title,

        @NotNull(message = "Debes asignar a un responsable de revisar la decision")
        Integer reviewedById,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        @NotNull(message = "La fecha limite es obligatoria")
        LocalDate deadline
) {}
