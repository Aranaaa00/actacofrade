package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateEventRequest(

        @Size(max = 255, message = "El nombre del acto no puede superar los 255 caracteres")
        String title,

        @Pattern(regexp = "CABILDO|CULTOS|PROCESION|ENSAYO|OTRO", message = "Tipo de acto no v\u00e1lido")
        String eventType,

        LocalDate eventDate,

        @Size(max = 255, message = "La ubicacion no puede superar los 255 caracteres")
        String location,

        Integer responsibleId,

        String observations
) {}
