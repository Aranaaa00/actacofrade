package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateEventRequest(

        @Size(max = 255, message = "El nombre del acto no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El nombre contiene caracteres no permitidos")
        String title,

        @Pattern(regexp = "CABILDO|CULTOS|PROCESION|ESTACION_PENITENCIA|ENSAYO|OTRO", message = "Tipo de acto no v\u00e1lido")
        String eventType,

        LocalDate eventDate,

        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La ubicación contiene caracteres no permitidos")
        String location,

        Integer responsibleId,

        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "Las observaciones contienen caracteres no permitidos")
        String observations
) {}
