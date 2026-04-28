package com.actacofrade.backend.dto;

import java.time.LocalDate;

public record DashboardAlertResponse(
        String type,
        String description,
        Integer eventId,
        LocalDate eventDate,
        Integer entityId
) {}
