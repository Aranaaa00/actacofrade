package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.DashboardAlertResponse;
import com.actacofrade.backend.dto.DashboardResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de DashboardController.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerIntegrationTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    @Test
    void getDashboard_returns200() throws Exception {
        DashboardAlertResponse alert = new DashboardAlertResponse(
                "INCIDENT_OPEN", "Incidencia sin resolver", 1, LocalDate.of(2030, 4, 1), 7);
        DashboardResponse resp = new DashboardResponse(List.of(), List.of(alert), 4, 2);
        given(dashboardService.getDashboard(anyString())).willReturn(resp);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingTasksCount").value(4))
                .andExpect(jsonPath("$.readyToCloseCount").value(2))
                .andExpect(jsonPath("$.alerts[0].type").value("INCIDENT_OPEN"));
    }
}
