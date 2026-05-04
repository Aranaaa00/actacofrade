package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.EventResponse;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP del EventController usando standaloneSetup.
 * Verifica códigos 200/204/404, formato JSON, mapeo de excepciones y
 * resolución de @AuthenticationPrincipal.
 */
@ExtendWith(MockitoExtension.class)
class EventControllerIntegrationTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Argument resolver propio para inyectar UserDetails (hace innecesario cargar Spring Security)
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.getParameterAnnotation(
                        org.springframework.security.core.annotation.AuthenticationPrincipal.class) != null;
            }
            @Override
            public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          org.springframework.web.context.request.NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return User.withUsername("admin@hermandad.es").password("x").roles("ADMINISTRADOR").build();
            }
        };
        mockMvc = MockMvcBuilders
                .standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    private EventResponse sample() {
        return new EventResponse(1, "EVT-001", "Salida procesional",
                "PROCESION", LocalDate.of(2026, 4, 3),
                "Sevilla", "obs", "PLANIFICACION",
                10, "Admin", false,
                3L, 0L, 5L, 2L,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void findAll_authenticated_returns200WithList() throws Exception {
        given(eventService.findAll(anyString())).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Salida procesional"));
    }

    @Test
    void findById_existingEvent_returns200() throws Exception {
        given(eventService.findById(eq(1), anyString())).willReturn(sample());

        mockMvc.perform(get("/api/events/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("EVT-001"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(eventService.findById(eq(999), anyString()))
                .willThrow(new IllegalArgumentException("Acto no encontrado"));

        mockMvc.perform(get("/api/events/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Acto no encontrado"));
    }

    @Test
    void delete_existingEvent_returns204() throws Exception {
        mockMvc.perform(delete("/api/events/{id}", 1))
                .andExpect(status().isNoContent());

        verify(eventService).delete(eq(1), anyString());
    }

    @Test
    void getAvailableDates_returns200WithJsonArray() throws Exception {
        given(eventService.getAvailableDates(anyString()))
                .willReturn(List.of("2026-04-03", "2026-04-10"));

        mockMvc.perform(get("/api/events/available-dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2026-04-03"))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
