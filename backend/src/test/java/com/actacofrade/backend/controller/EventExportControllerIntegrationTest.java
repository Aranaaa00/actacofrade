package com.actacofrade.backend.controller;

import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.EventExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de EventExportController.
 */
@ExtendWith(MockitoExtension.class)
class EventExportControllerIntegrationTest {

    @Mock
    private EventExportService eventExportService;

    @InjectMocks
    private EventExportController eventExportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(eventExportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    @Test
    void export_pdf_returns200WithPdfHeaders() throws Exception {
        byte[] payload = new byte[]{0x25, 0x50, 0x44, 0x46};
        given(eventExportService.export(eq(1), any(), anyString())).willReturn(payload);

        String body = "{\"format\":\"PDF\",\"selectedSections\":[\"TASKS\"]}";

        mockMvc.perform(post("/api/events/{id}/export", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"acto-1.pdf\""))
                .andExpect(content().bytes(payload));
    }

    @Test
    void export_csv_returns200WithCsvHeaders() throws Exception {
        byte[] payload = "id;title\n1;X".getBytes();
        given(eventExportService.export(eq(2), any(), anyString())).willReturn(payload);

        String body = "{\"format\":\"CSV\",\"selectedSections\":[\"TASKS\"]}";

        mockMvc.perform(post("/api/events/{id}/export", 2)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"acto-2.csv\""));
    }

    @Test
    void export_invalidFormat_returns400() throws Exception {
        String body = "{\"format\":\"XML\",\"selectedSections\":[\"TASKS\"]}";

        mockMvc.perform(post("/api/events/{id}/export", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void export_emptySections_returns400() throws Exception {
        String body = "{\"format\":\"PDF\",\"selectedSections\":[]}";

        mockMvc.perform(post("/api/events/{id}/export", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
