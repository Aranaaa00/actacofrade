package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AuditLogResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de AuditLogController.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogControllerIntegrationTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(auditLogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        TestSupport.authPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void findByEventId_returnsPage() throws Exception {
        AuditLogResponse log = new AuditLogResponse(1, 5, "TASK", 10,
                "CREATED", 2, "Admin", LocalDateTime.now(), "Tarea creada");
        Page<AuditLogResponse> page = new PageImpl<>(List.of(log), PageRequest.of(0, 5), 1);
        given(auditLogService.findByEventId(eq(5), any(), anyString())).willReturn(page);

        mockMvc.perform(get("/api/events/{eventId}/history", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CREATED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findByEventId_eventNotFound_returns404() throws Exception {
        given(auditLogService.findByEventId(eq(99), any(), anyString()))
                .willThrow(new IllegalArgumentException("Acto no encontrado"));

        mockMvc.perform(get("/api/events/{eventId}/history", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Acto no encontrado"));
    }
}
