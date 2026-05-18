package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.InterventionLogEntry;
import com.actacofrade.backend.dto.PageResponse;
import com.actacofrade.backend.dto.SuperAdminRoleRequest;
import com.actacofrade.backend.dto.SuperAdminStatusRequest;
import com.actacofrade.backend.dto.SuperAdminUserResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.SuperAdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuperAdminUserControllerIntegrationTest {

    @Mock private SuperAdminUserService service;
    @InjectMocks private SuperAdminUserController controller;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper();

    private SuperAdminUserResponse sample() {
        return new SuperAdminUserResponse(2, "User 2", "user@e.com", List.of("COLABORADOR"),
                "ACTIVE", null, null, false, null, "H", null, LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    @Test
    void search_returnsOk() throws Exception {
        given(service.search(any(), anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(sample()), 0, 20, 1, 1));
        mockMvc.perform(get("/api/super-admin/users").param("query", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@e.com"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        given(service.findById(2)).willReturn(sample());
        mockMvc.perform(get("/api/super-admin/users/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void findById_notFound() throws Exception {
        given(service.findById(99)).willThrow(new IllegalArgumentException("Usuario no encontrado"));
        mockMvc.perform(get("/api/super-admin/users/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_returnsOk() throws Exception {
        given(service.updateStatus(eq(2), any(), anyString())).willReturn(sample());
        mockMvc.perform(patch("/api/super-admin/users/{id}/status", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SuperAdminStatusRequest("SUSPENDED", "razon"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_forbidden() throws Exception {
        given(service.updateStatus(eq(3), any(), anyString())).willThrow(new AccessDeniedException("no"));
        mockMvc.perform(patch("/api/super-admin/users/{id}/status", 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SuperAdminStatusRequest("ACTIVE", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void verify_returnsOk() throws Exception {
        given(service.setManualVerification(eq(2), eq(true), anyString())).willReturn(sample());
        mockMvc.perform(post("/api/super-admin/users/{id}/verify", 2))
                .andExpect(status().isOk());
    }

    @Test
    void unverify_returnsOk() throws Exception {
        given(service.setManualVerification(eq(2), eq(false), anyString())).willReturn(sample());
        mockMvc.perform(post("/api/super-admin/users/{id}/unverify", 2))
                .andExpect(status().isOk());
    }

    @Test
    void overrideRole_returnsOk() throws Exception {
        given(service.overrideRole(eq(2), any(), anyString())).willReturn(sample());
        mockMvc.perform(patch("/api/super-admin/users/{id}/role", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SuperAdminRoleRequest("RESPONSABLE", "motivo claro"))))
                .andExpect(status().isOk());
    }

    @Test
    void triggerPasswordReset_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/super-admin/users/{id}/password-reset", 2))
                .andExpect(status().isNoContent());
    }

    @Test
    void triggerPasswordReset_emailFails_returns409() throws Exception {
        doThrow(new IllegalStateException("fallo"))
                .when(service).triggerPasswordReset(eq(2), anyString());
        mockMvc.perform(post("/api/super-admin/users/{id}/password-reset", 2))
                .andExpect(status().isConflict());
    }

    @Test
    void userLogs_returnsOk() throws Exception {
        InterventionLogEntry e = new InterventionLogEntry(1, "SUPERADMIN_STATUS_CHANGE", 2, 100,
                "Super", LocalDateTime.now(), "d", "{}");
        given(service.findUserLogs(eq(2), anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(e), 0, 20, 1, 1));
        mockMvc.perform(get("/api/super-admin/users/{id}/logs", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("SUPERADMIN_STATUS_CHANGE"));
    }

    @Test
    void allLogs_returnsOk() throws Exception {
        given(service.findAllLogs(anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/super-admin/users/logs"))
                .andExpect(status().isOk());
    }
}
