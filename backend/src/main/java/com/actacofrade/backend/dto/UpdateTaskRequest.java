package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(

        @Size(max = 255, message = "El titulo de la tarea no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El título contiene caracteres no permitidos")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        Integer assignedToId,

        LocalDate deadline
) {}
