package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP del AuthController usando standaloneSetup.
 * Verifica códigos de estado, formato JSON, validación Jakarta y mapeo
 * de excepciones a través del GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        AuthResponse resp = new AuthResponse(1, "TOKEN", "admin@hermandad.es",
                "Admin Hermandad", List.of("ADMINISTRADOR"), "Hermandad de Prueba", false);
        given(authService.register(any())).willReturn(resp);

        String body = """
                {
                  "fullName": "Admin Hermandad",
                  "email": "admin@hermandad.es",
                  "password": "Pass1234.",
                  "roleCode": "ADMINISTRADOR",
                  "hermandadNombre": "Hermandad de Prueba"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("TOKEN"))
                .andExpect(jsonPath("$.email").value("admin@hermandad.es"))
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        String body = """
                {
                  "fullName": "Admin Hermandad",
                  "email": "no-es-email",
                  "password": "Pass1234.",
                  "roleCode": "ADMINISTRADOR",
                  "hermandadNombre": "Hermandad de Prueba"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        given(authService.register(any()))
                .willThrow(new IllegalStateException("El email ya está registrado"));

        String body = """
                {
                  "fullName": "Admin Hermandad",
                  "email": "admin@hermandad.es",
                  "password": "Pass1234.",
                  "roleCode": "ADMINISTRADOR",
                  "hermandadNombre": "Hermandad de Prueba"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        AuthResponse resp = new AuthResponse(1, "TOKEN", "admin@hermandad.es",
                "Admin", List.of("ADMINISTRADOR"), "Hermandad", false);
        given(authService.login(any())).willReturn(resp);

        String body = "{\"email\":\"admin@hermandad.es\",\"password\":\"Pass1234.\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        given(authService.login(any())).willThrow(new BadCredentialsException("bad"));

        String body = "{\"email\":\"admin@hermandad.es\",\"password\":\"WrongPass1.\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    void login_malformedBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verifyNoInteractions(authService);
    }
}
