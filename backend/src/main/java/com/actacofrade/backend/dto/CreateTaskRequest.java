package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(

        @NotBlank(message = "El titulo de la tarea es obligatorio")
        @Size(max = 255, message = "El titulo de la tarea no puede superar los 255 caracteres")
        String title,

        String description,

        Integer assignedToId,

        LocalDate deadline
) {}
