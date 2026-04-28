package com.actacofrade.backend.dto;

import java.util.List;

public record DashboardResponse(
        List<EventResponse> recentEvents,
        List<DashboardAlertResponse> alerts,
        long pendingTasksCount,
        long readyToCloseCount
) {}
