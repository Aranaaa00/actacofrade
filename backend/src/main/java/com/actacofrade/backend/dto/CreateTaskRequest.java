package com.actacofrade.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(

        @NotBlank(message = "El titulo de la tarea es obligatorio")
        @Size(max = 255, message = "El titulo de la tarea no puede superar los 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El título contiene caracteres no permitidos")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción contiene caracteres no permitidos")
        String description,

        Integer assignedToId,

        @FutureOrPresent(message = "La fecha limite no puede ser anterior a hoy")
        LocalDate deadline
) {}
