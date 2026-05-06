package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.ChangePasswordRequest;
import com.actacofrade.backend.dto.UpdateProfileRequest;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.entity.UserAvatar;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HermandadService hermandadService;

    private MeService service;

    private Hermandad hermandad;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new MeService(userRepository, userAvatarRepository, passwordEncoder, hermandadService, 1024L,
                "image/png,image/jpeg");
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
    }

    @Test
    void me_returnsResponse() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        UserResponse res = service.me("admin@e.com");
        assertThat(res.email()).isEqualTo("admin@e.com");
    }

    @Test
    void me_unknownUser_throws() {
        when(userRepository.findByEmail("nope@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.me("nope@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateProfile_changesEmailAndName() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("nuevo@e.com")).thenReturn(false);
        UpdateProfileRequest req = new UpdateProfileRequest("Nuevo Nombre", "Nuevo@E.com");
        UserResponse res = service.updateProfile(req, "admin@e.com");
        assertThat(res.fullName()).isEqualTo("Nuevo Nombre");
        assertThat(res.email()).isEqualTo("nuevo@e.com");
    }

    @Test
    void updateProfile_emailAlreadyUsed_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("nuevo@e.com")).thenReturn(true);
        UpdateProfileRequest req = new UpdateProfileRequest("Nombre Valido", "nuevo@e.com");
        assertThatThrownBy(() -> service.updateProfile(req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("bad", admin.getPasswordHash())).thenReturn(false);
        ChangePasswordRequest req = new ChangePasswordRequest("bad", "Pass1234.");
        assertThatThrownBy(() -> service.changePassword(req, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void changePassword_sameAsCurrent_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("good", admin.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("Pass1234.", admin.getPasswordHash())).thenReturn(true);
        ChangePasswordRequest req = new ChangePasswordRequest("good", "Pass1234.");
        assertThatThrownBy(() -> service.changePassword(req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changePassword_success() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("good", admin.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("Pass1234.", admin.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("Pass1234.")).thenReturn("NEW_HASH");
        service.changePassword(new ChangePasswordRequest("good", "Pass1234."), "admin@e.com");
        assertThat(admin.getPasswordHash()).isEqualTo("NEW_HASH");
    }

    @Test
    void uploadAvatar_emptyFile_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        MockMultipartFile file = new MockMultipartFile("avatar", new byte[0]);
        assertThatThrownBy(() -> service.uploadAvatar(file, "admin@e.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadAvatar_tooLarge_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        MockMultipartFile file = new MockMultipartFile("avatar", "x.png", "image/png", new byte[2048]);
        assertThatThrownBy(() -> service.uploadAvatar(file, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadAvatar_invalidContentType_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        MockMultipartFile file = new MockMultipartFile("avatar", "x.gif", "image/gif", new byte[10]);
        assertThatThrownBy(() -> service.uploadAvatar(file, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadAvatar_validFile_persists() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userAvatarRepository.findByUserId(1)).thenReturn(Optional.empty());
        // Use real PNG signature bytes to pass magic byte validation.
        byte[] png = new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0
        };
        MockMultipartFile file = new MockMultipartFile("avatar", "x.png", "image/png", png);
        service.uploadAvatar(file, "admin@e.com");
        verify(userAvatarRepository).save(any(UserAvatar.class));
    }

    @Test
    void uploadAvatar_spoofedContentType_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        // Declared as PNG but contents are not a real PNG.
        MockMultipartFile file = new MockMultipartFile("avatar", "x.png", "image/png", new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        assertThatThrownBy(() -> service.uploadAvatar(file, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteAvatar_invokesRepository() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        service.deleteAvatar("admin@e.com");
        verify(userAvatarRepository).deleteByUserId(1);
    }

    @Test
    void getAvatar_otherHermandad_denied() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndHermandadId(99, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAvatar(99, "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(userAvatarRepository, never()).findByUserId(any());
    }

    @Test
    void getAvatar_userWithoutHermandad_throws() {
        User u = TestFixtures.user(2, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.getAvatar(1, "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }
}
