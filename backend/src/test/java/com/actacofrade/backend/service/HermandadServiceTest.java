package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.HermandadResponse;
import com.actacofrade.backend.dto.HermandadUpdateRequest;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AdminChangeRequestRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HermandadServiceTest {

    @Mock private HermandadRepository hermandadRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private AdminChangeRequestRepository adminChangeRequestRepository;

    private HermandadService service;

    private Hermandad hermandad;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new HermandadService(hermandadRepository, userRepository, eventRepository,
                userAvatarRepository, adminChangeRequestRepository);
        hermandad = TestFixtures.hermandad(1, "Original");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
    }

    @Test
    void getCurrent_returnsResponse() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(userRepository.countByHermandadIdAndActiveTrue(1)).thenReturn(3L);
        when(eventRepository.countByHermandadId(1)).thenReturn(7L);

        HermandadResponse res = service.getCurrent("admin@e.com");
        assertThat(res.id()).isEqualTo(1);
        assertThat(res.usersCount()).isEqualTo(3);
        assertThat(res.eventsCount()).isEqualTo(7);
    }

    @Test
    void getCurrent_userWithoutHermandad_throws() {
        User u = TestFixtures.user(2, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.getCurrent("x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateCurrent_changesAllFields() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        HermandadUpdateRequest req = new HermandadUpdateRequest(
                "Nueva", "desc", 1900, "Sevilla", "Calle 1",
                "Mail@E.com", "+34 600 000 000", "https://example.com");

        HermandadResponse res = service.updateCurrent(req, "admin@e.com");
        assertThat(res.nombre()).isEqualTo("Nueva");
        assertThat(res.emailContacto()).isEqualTo("mail@e.com");
        assertThat(hermandad.getAnioFundacion()).isEqualTo(1900);
    }

    @Test
    void updateCurrent_duplicateName_throws() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(hermandadRepository.existsByNombreIgnoreCase("Otra")).thenReturn(true);
        HermandadUpdateRequest req = new HermandadUpdateRequest("Otra", null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.updateCurrent(req, "admin@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateCurrent_sameNameDifferentCase_allowed() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        HermandadUpdateRequest req = new HermandadUpdateRequest("ORIGINAL", null, null, null, null, null, null, null);
        HermandadResponse res = service.updateCurrent(req, "admin@e.com");
        assertThat(res.nombre()).isEqualTo("ORIGINAL");
    }
}
