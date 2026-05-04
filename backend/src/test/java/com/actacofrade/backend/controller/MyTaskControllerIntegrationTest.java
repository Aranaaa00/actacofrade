package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.MyTaskResponse;
import com.actacofrade.backend.dto.MyTaskStatsResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
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
 * Tests de integración HTTP de MyTaskController.
 */
@ExtendWith(MockitoExtension.class)
class MyTaskControllerIntegrationTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private MyTaskController myTaskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(myTaskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    @Test
    void findMyTasks_returnsPage() throws Exception {
        MyTaskResponse t = new MyTaskResponse(1, 5, "PROCESION", "Salida",
                "Llevar varales", "ACEPTADA", LocalDate.of(2030, 4, 1),
                null, LocalDateTime.now(), null, LocalDateTime.now());
        Page<MyTaskResponse> page = new PageImpl<>(List.of(t), PageRequest.of(0, 5), 1);
        given(taskService.findMyTasks(anyString(), any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/my-tasks").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].eventTitle").value("Salida"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMyTaskStats_returns200() throws Exception {
        given(taskService.getMyTaskStats(eq("admin@hermandad.es")))
                .willReturn(new MyTaskStatsResponse(3, 5, 1));

        mockMvc.perform(get("/api/my-tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(3))
                .andExpect(jsonPath("$.confirmedCount").value(5))
                .andExpect(jsonPath("$.rejectedCount").value(1));
    }
}
