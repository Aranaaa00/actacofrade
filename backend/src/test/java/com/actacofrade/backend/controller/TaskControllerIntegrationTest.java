package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.TaskResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
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
 * Tests de integración HTTP de TaskController.
 */
@ExtendWith(MockitoExtension.class)
class TaskControllerIntegrationTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private TaskResponse sample() {
        return new TaskResponse(10, 1, "Tarea X", "desc", 5, "Juan", 2,
                "PLANIFICADA", LocalDate.of(2030, 4, 1), null, null, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void findByEventId_returns200WithList() throws Exception {
        given(taskService.findByEventId(eq(1), anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/events/{eventId}/tasks", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Tarea X"));
    }

    @Test
    void findById_returns200() throws Exception {
        given(taskService.findById(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(get("/api/events/{eventId}/tasks/{taskId}", 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(taskService.findById(eq(1), eq(99), anyString()))
                .willThrow(new IllegalArgumentException("Tarea no encontrada"));

        mockMvc.perform(get("/api/events/{eventId}/tasks/{taskId}", 1, 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tarea no encontrada"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(taskService.create(eq(1), any(), anyString())).willReturn(sample());

        String body = """
                {"title":"Tarea X","description":"desc","assignedToId":5,"deadline":"2030-04-01"}
                """;

        mockMvc.perform(post("/api/events/{eventId}/tasks", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void update_returns200() throws Exception {
        given(taskService.update(eq(1), eq(10), any(), anyString())).willReturn(sample());
        String body = """
                {"title":"Tarea X","description":"desc","assignedToId":5,"deadline":"2030-04-01"}
                """;

        mockMvc.perform(put("/api/events/{eventId}/tasks/{taskId}", 1, 10)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}/tasks/{taskId}", 1, 10))
                .andExpect(status().isNoContent());
        verify(taskService).delete(eq(1), eq(10), anyString());
    }

    @Test
    void accept_returns200() throws Exception {
        given(taskService.accept(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/accept", 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void startPreparation_returns200() throws Exception {
        given(taskService.startPreparation(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/start-preparation", 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void confirm_returns200() throws Exception {
        given(taskService.confirm(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/confirm", 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void complete_returns200() throws Exception {
        given(taskService.complete(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/complete", 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void reject_returns200() throws Exception {
        given(taskService.reject(eq(1), eq(10), eq("Motivo válido aquí"), anyString())).willReturn(sample());
        String body = "{\"rejectionReason\":\"Motivo válido aquí\"}";

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/reject", 1, 10)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void resetToPlanned_returns200() throws Exception {
        given(taskService.resetToPlanned(eq(1), eq(10), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/events/{eventId}/tasks/{taskId}/reset", 1, 10))
                .andExpect(status().isOk());
    }
}
