package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIncidentRequest(

        @NotBlank(message = "La descripcion de la incidencia es obligatoria")
        String description,

        Integer reportedById
) {}
