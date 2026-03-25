package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEventRequest(

        @NotBlank(message = "El nombre del acto es obligatorio")
        @Size(max = 255, message = "El nombre del acto no puede superar los 255 caracteres")
        String title,

        @NotBlank(message = "El tipo de acto es obligatorio")
        String eventType,

        @NotNull(message = "La fecha del acto es obligatoria")
        LocalDate eventDate,

        @Size(max = 255, message = "La ubicacion no puede superar los 255 caracteres")
        String location,

        Integer responsibleId,

        String observations
) {}
