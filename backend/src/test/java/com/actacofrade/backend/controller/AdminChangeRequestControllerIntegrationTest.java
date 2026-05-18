package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AdminChangeRequestApprove;
import com.actacofrade.backend.dto.AdminChangeRequestCreate;
import com.actacofrade.backend.dto.AdminChangeRequestResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.SupportRequestType;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.AdminChangeRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminChangeRequestControllerIntegrationTest {

    @Mock private AdminChangeRequestService service;
    @InjectMocks private AdminChangeRequestController controller;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper();

    private AdminChangeRequestResponse sample(String status) {
        return new AdminChangeRequestResponse(1, "ADMIN_CHANGE", 1, "H",
                10, "Requester", "req@e.com", "mensaje", status,
                null, null, null, null, OffsetDateTime.now());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        given(service.create(any(), anyString())).willReturn(sample("PENDING"));
        mockMvc.perform(post("/api/admin-change-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminChangeRequestCreate(
                                SupportRequestType.ADMIN_CHANGE, "mensaje suficientemente largo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findAll_returnsOk() throws Exception {
        given(service.findAll()).willReturn(List.of(sample("PENDING")));
        mockMvc.perform(get("/api/admin-change-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void findById_returnsOk() throws Exception {
        given(service.findById(1)).willReturn(sample("PENDING"));
        mockMvc.perform(get("/api/admin-change-requests/{id}", 1))
                .andExpect(status().isOk());
    }

    @Test
    void findById_notFound() throws Exception {
        given(service.findById(99)).willThrow(new IllegalArgumentException("Solicitud no encontrada"));
        mockMvc.perform(get("/api/admin-change-requests/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    void findCandidates_returnsOk() throws Exception {
        UserResponse u = new UserResponse(11, "Cand", "cand@e.com", List.of("COLABORADOR"),
                true, null, false, false);
        given(service.findCandidates(1)).willReturn(List.of(u));
        mockMvc.perform(get("/api/admin-change-requests/{id}/candidates", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    @Test
    void approve_returnsOk() throws Exception {
        given(service.approve(eq(1), any(), anyString())).willReturn(sample("APPROVED"));
        mockMvc.perform(patch("/api/admin-change-requests/{id}/approve", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminChangeRequestApprove(11))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_invalidStateReturnsConflict() throws Exception {
        given(service.approve(eq(1), any(), anyString()))
                .willThrow(new IllegalStateException("ya resuelta"));
        mockMvc.perform(patch("/api/admin-change-requests/{id}/approve", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminChangeRequestApprove(11))))
                .andExpect(status().isConflict());
    }

    @Test
    void reject_returnsOk() throws Exception {
        given(service.reject(eq(1), anyString())).willReturn(sample("REJECTED"));
        mockMvc.perform(patch("/api/admin-change-requests/{id}/reject", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void resolve_returnsOk() throws Exception {
        given(service.resolve(eq(1), anyString())).willReturn(sample("APPROVED"));
        mockMvc.perform(patch("/api/admin-change-requests/{id}/resolve", 1))
                .andExpect(status().isOk());
    }
}
