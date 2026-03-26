package com.actacofrade.backend.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(

        @Size(max = 255, message = "El titulo de la tarea no puede superar los 255 caracteres")
        String title,

        String description,

        Integer assignedToId,

        LocalDate deadline
) {}
