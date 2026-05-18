package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateDecisionRequest(

        @Pattern(regexp = "MAYORDOMIA|SECRETARIA|PRIOSTIA|TESORERIA|DIPUTACION_MAYOR", message = "\u00c1rea no v\u00e1lida")
        String area,

        @Size(max = 255, message = "El titulo de la decision no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El título contiene caracteres no permitidos")
        String title,

        Integer reviewedById,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        LocalDate deadline
) {}
