package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateDecisionRequest(

        String area,

        @Size(max = 255, message = "El titulo de la decision no puede superar los 255 caracteres")
        String title,

        Integer reviewedById
) {}
