package com.actacofrade.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ExportRequest(
        @NotBlank
        @Pattern(regexp = "PDF|CSV", message = "El formato debe ser PDF o CSV")
        String format,

        @NotEmpty(message = "Debe seleccionar al menos una sección")
        List<String> selectedSections
) {}
