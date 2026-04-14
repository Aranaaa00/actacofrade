package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDecisionRequest(

        @Pattern(regexp = "MAYORDOMIA|SECRETARIA|PRIOSTIA|TESORERIA|DIPUTACION_MAYOR", message = "\u00c1rea no v\u00e1lida")
        String area,

        @Size(max = 255, message = "El titulo de la decision no puede superar los 255 caracteres")
        String title,

        Integer reviewedById
) {}
