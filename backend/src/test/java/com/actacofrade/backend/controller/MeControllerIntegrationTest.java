package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.UserAvatar;
import com.actacofrade.backend.exception.GlobalExceptionHandler;
import com.actacofrade.backend.service.MeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración HTTP de MeController.
 */
@ExtendWith(MockitoExtension.class)
class MeControllerIntegrationTest {

    @Mock
    private MeService meService;

    @InjectMocks
    private MeController meController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(meController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestSupport.authPrincipalResolver())
                .build();
    }

    private UserResponse sample() {
        return new UserResponse(1, "Admin Hermandad", "admin@hermandad.es",
                List.of("ADMINISTRADOR"), true, LocalDateTime.now(), false);
    }

    @Test
    void me_returns200() throws Exception {
        given(meService.me(anyString())).willReturn(sample());

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@hermandad.es"));
    }

    @Test
    void updateProfile_returns200() throws Exception {
        given(meService.updateProfile(any(), anyString())).willReturn(sample());
        String body = "{\"fullName\":\"Admin Hermandad\",\"email\":\"admin@hermandad.es\"}";

        mockMvc.perform(put("/api/me")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        String body = "{\"fullName\":\"Admin Hermandad\",\"email\":\"no-email\"}";

        mockMvc.perform(put("/api/me")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_returns204() throws Exception {
        String body = "{\"currentPassword\":\"OldPass1.\",\"newPassword\":\"NewPass1.\"}";

        mockMvc.perform(patch("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        verify(meService).changePassword(any(), anyString());
    }

    @Test
    void uploadAvatar_returns200() throws Exception {
        given(meService.uploadAvatar(any(), anyString())).willReturn(sample());

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png",
                MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@hermandad.es"));
    }

    @Test
    void deleteAvatar_returns204() throws Exception {
        mockMvc.perform(delete("/api/me/avatar"))
                .andExpect(status().isNoContent());
        verify(meService).deleteAvatar(anyString());
    }

    @Test
    void getUserAvatar_existing_returns200WithBytes() throws Exception {
        UserAvatar avatar = new UserAvatar();
        avatar.setData(new byte[]{1, 2, 3});
        avatar.setContentType("image/png");
        given(meService.getAvatar(eq(1), anyString())).willReturn(Optional.of(avatar));

        mockMvc.perform(get("/api/me/avatar/{userId}", 1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "private, max-age=300"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void getUserAvatar_missing_returns404() throws Exception {
        given(meService.getAvatar(eq(99), anyString())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/me/avatar/{userId}", 99))
                .andExpect(status().isNotFound());
    }
}
