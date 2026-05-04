package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.UserService;
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
 * Tests de integración HTTP de UserController.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerIntegrationTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private UserResponse sample() {
        return new UserResponse(5, "Juan Pérez", "juan@hermandad.es",
                List.of("RESPONSABLE"), true, LocalDateTime.now(), false);
    }

    @Test
    void findAll_returns200() throws Exception {
        given(userService.findAll(anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(userService.create(any(), anyString())).willReturn(sample());
        String body = """
                {
                  "fullName":"Juan Pérez",
                  "email":"juan@hermandad.es",
                  "password":"Pass1234.",
                  "roleCode":"RESPONSABLE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        String body = """
                {
                  "fullName":"Juan Pérez",
                  "email":"no-es-email",
                  "password":"Pass1234.",
                  "roleCode":"RESPONSABLE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAssignable_returns200() throws Exception {
        given(userService.findAssignable(anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/users/assignable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returns200() throws Exception {
        given(userService.findById(eq(5), anyString())).willReturn(sample());

        mockMvc.perform(get("/api/users/{id}", 5))
                .andExpect(status().isOk());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(userService.findById(eq(99), anyString()))
                .willThrow(new IllegalArgumentException("Usuario no encontrado"));

        mockMvc.perform(get("/api/users/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void getStats_returns200() throws Exception {
        given(userService.countByRole(anyString())).willReturn(new RoleStatsResponse(1, 2, 3, 4));

        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administradores").value(1))
                .andExpect(jsonPath("$.consulta").value(4));
    }

    @Test
    void update_returns200() throws Exception {
        given(userService.update(eq(5), any(), anyString())).willReturn(sample());
        String body = "{\"fullName\":\"Juan Pérez\",\"email\":\"juan@hermandad.es\",\"roleCode\":\"COLABORADOR\"}";

        mockMvc.perform(put("/api/users/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void toggleActive_returns200() throws Exception {
        given(userService.toggleActive(eq(5), anyString())).willReturn(sample());

        mockMvc.perform(patch("/api/users/{id}/toggle-active", 5))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 5))
                .andExpect(status().isNoContent());
        verify(userService).delete(eq(5), anyString());
    }
}
