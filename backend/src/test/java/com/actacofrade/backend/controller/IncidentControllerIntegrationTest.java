package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.IncidentResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de IncidentController.
 */
@ExtendWith(MockitoExtension.class)
class IncidentControllerIntegrationTest {

    @Mock
    private IncidentService incidentService;

    @InjectMocks
    private IncidentController incidentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private IncidentResponse sample() {
        return new IncidentResponse(7, 1, "Caída del paso", "ABIERTA",
                3, "Reportador", null, null, LocalDateTime.now(), null);
    }

    @Test
    void findByEventId_returns200() throws Exception {
        given(incidentService.findByEventId(eq(1), anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/events/{eventId}/incidents", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void findById_returns200() throws Exception {
        given(incidentService.findById(eq(1), eq(7), anyString())).willReturn(sample());

        mockMvc.perform(get("/api/events/{eventId}/incidents/{id}", 1, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABIERTA"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(incidentService.findById(eq(1), eq(99), anyString()))
                .willThrow(new IllegalArgumentException("Incidencia no encontrada"));

        mockMvc.perform(get("/api/events/{eventId}/incidents/{id}", 1, 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(incidentService.create(eq(1), any(), anyString())).willReturn(sample());
        String body = "{\"description\":\"Caída del paso\",\"reportedById\":3}";

        mockMvc.perform(post("/api/events/{eventId}/incidents", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        String body = "{\"description\":\"\",\"reportedById\":3}";

        mockMvc.perform(post("/api/events/{eventId}/incidents", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}/incidents/{id}", 1, 7))
                .andExpect(status().isNoContent());
        verify(incidentService).delete(eq(1), eq(7), anyString());
    }

    @Test
    void resolve_returns200() throws Exception {
        given(incidentService.resolve(eq(1), eq(7), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/incidents/{id}/resolve", 1, 7))
                .andExpect(status().isOk());
    }

    @Test
    void reopen_returns200() throws Exception {
        given(incidentService.reopen(eq(1), eq(7), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/incidents/{id}/reopen", 1, 7))
                .andExpect(status().isOk());
    }
}
