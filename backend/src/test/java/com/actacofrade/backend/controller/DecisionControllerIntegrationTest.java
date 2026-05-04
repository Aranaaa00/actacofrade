package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.DecisionResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.DecisionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de DecisionController.
 */
@ExtendWith(MockitoExtension.class)
class DecisionControllerIntegrationTest {

    @Mock
    private DecisionService decisionService;

    @InjectMocks
    private DecisionController decisionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(decisionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private DecisionResponse sample() {
        return new DecisionResponse(4, 1, "MAYORDOMIA", "Comprar varales",
                "PROPUESTA", null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void findByEventId_returns200() throws Exception {
        given(decisionService.findByEventId(eq(1), anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/events/{eventId}/decisions", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].area").value("MAYORDOMIA"));
    }

    @Test
    void findById_returns200() throws Exception {
        given(decisionService.findById(eq(1), eq(4), anyString())).willReturn(sample());

        mockMvc.perform(get("/api/events/{eventId}/decisions/{id}", 1, 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(decisionService.create(eq(1), any(), anyString())).willReturn(sample());
        String body = "{\"area\":\"MAYORDOMIA\",\"title\":\"Comprar varales\",\"reviewedById\":null}";

        mockMvc.perform(post("/api/events/{eventId}/decisions", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Comprar varales"));
    }

    @Test
    void create_invalidArea_returns400() throws Exception {
        String body = "{\"area\":\"INVALIDA\",\"title\":\"X\",\"reviewedById\":null}";

        mockMvc.perform(post("/api/events/{eventId}/decisions", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        given(decisionService.update(eq(1), eq(4), any(), anyString())).willReturn(sample());
        String body = "{\"area\":\"MAYORDOMIA\",\"title\":\"Comprar varales\",\"reviewedById\":null}";

        mockMvc.perform(put("/api/events/{eventId}/decisions/{id}", 1, 4)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}/decisions/{id}", 1, 4))
                .andExpect(status().isNoContent());
        verify(decisionService).delete(eq(1), eq(4), anyString());
    }

    @Test
    void accept_returns200() throws Exception {
        given(decisionService.accept(eq(1), eq(4), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/decisions/{id}/accept", 1, 4))
                .andExpect(status().isOk());
    }

    @Test
    void reject_returns200() throws Exception {
        given(decisionService.reject(eq(1), eq(4), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/decisions/{id}/reject", 1, 4))
                .andExpect(status().isOk());
    }
}
