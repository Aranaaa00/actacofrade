package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.HermandadResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.HermandadService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de HermandadController.
 */
@ExtendWith(MockitoExtension.class)
class HermandadControllerIntegrationTest {

    @Mock
    private HermandadService hermandadService;

    @InjectMocks
    private HermandadController hermandadController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(hermandadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private HermandadResponse sample() {
        return new HermandadResponse(1, "Hermandad de Prueba", "Descripción", 1900,
                "Sevilla", "Calle Sol 1", "contacto@hermandad.es", "+34 600000000",
                "https://hermandad.es", LocalDateTime.now(), LocalDateTime.now(), 5, 12);
    }

    @Test
    void getCurrent_returns200() throws Exception {
        given(hermandadService.getCurrent(anyString())).willReturn(sample());

        mockMvc.perform(get("/api/hermandades/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hermandad de Prueba"))
                .andExpect(jsonPath("$.usersCount").value(5));
    }

    @Test
    void updateCurrent_validRequest_returns200() throws Exception {
        given(hermandadService.updateCurrent(any(), anyString())).willReturn(sample());
        String body = """
                {
                  "nombre":"Hermandad de Prueba",
                  "descripcion":"Descripción",
                  "anioFundacion":1900,
                  "localidad":"Sevilla",
                  "direccionSede":"Calle Sol 1",
                  "emailContacto":"contacto@hermandad.es",
                  "telefonoContacto":"+34 600000000",
                  "sitioWeb":"https://hermandad.es"
                }
                """;

        mockMvc.perform(put("/api/hermandades/me")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateCurrent_invalidYear_returns400() throws Exception {
        String body = """
                {
                  "nombre":"Hermandad de Prueba",
                  "anioFundacion":500
                }
                """;

        mockMvc.perform(put("/api/hermandades/me")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
