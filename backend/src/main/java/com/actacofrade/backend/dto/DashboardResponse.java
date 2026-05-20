package com.actacofrade.backend.dto;

import java.util.List;

public record DashboardResponse(
        List<EventResponse> recentEvents,
        List<DashboardAlertResponse> alerts,
        long pendingItemsCount,
        long readyToCloseCount,
        long totalEventsCount,
        long myTasksCount,
        long hermandadPendingCount
) {}
