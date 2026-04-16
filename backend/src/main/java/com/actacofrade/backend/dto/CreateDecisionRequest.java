package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDecisionRequest(

        @NotBlank(message = "El area de la decision es obligatoria")
        @Pattern(regexp = "MAYORDOMIA|SECRETARIA|PRIOSTIA|TESORERIA|DIPUTACION_MAYOR", message = "\u00c1rea no v\u00e1lida")
        String area,

        @NotBlank(message = "El titulo de la decision es obligatorio")
        @Size(max = 255, message = "El titulo de la decision no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El título contiene caracteres no permitidos")
        String title,

        Integer reviewedById
) {}
