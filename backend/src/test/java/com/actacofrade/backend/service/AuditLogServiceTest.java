package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuditLogResponse;
import com.actacofrade.backend.entity.AuditLog;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AuditLogRepository;
import com.actacofrade.backend.repository.EventRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;

    private AuditLogService service;

    private Hermandad hermandad;
    private User admin;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, eventRepository, userRepository);
        hermandad = TestFixtures.hermandad(1, "H");
        admin = TestFixtures.user(1, "admin@e.com", hermandad, RoleCode.ADMINISTRADOR);
        event = TestFixtures.event(10, hermandad, admin);
    }

    @Test
    void findByEventId_returnsPageMapped() {
        AuditLog log = new AuditLog();
        log.setId(99);
        log.setEvent(event);
        log.setEntityType("TASK");
        log.setEntityId(20);
        log.setAction("CREATE");
        log.setPerformedBy(admin);
        log.setPerformedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(auditLogRepository.findByEventIdOrderByPerformedAtDesc(10, pageable)).thenReturn(page);

        Page<AuditLogResponse> result = service.findByEventId(10, pageable, "admin@e.com");
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).performedById()).isEqualTo(1);
    }

    @Test
    void findByEventId_eventNotInHermandad_denied() {
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByIdAndHermandadId(99, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByEventId(99, PageRequest.of(0, 10), "admin@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findByEventId_userWithoutHermandad_throws() {
        User u = TestFixtures.user(2, "x@e.com", null, RoleCode.ADMINISTRADOR);
        when(userRepository.findByEmail("x@e.com")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.findByEventId(10, PageRequest.of(0, 10), "x@e.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void log_persistsEntry() {
        service.log(event, "TASK", 20, "CREATE", admin, "details");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEvent()).isEqualTo(event);
        assertThat(saved.getEntityType()).isEqualTo("TASK");
        assertThat(saved.getEntityId()).isEqualTo(20);
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getPerformedBy()).isEqualTo(admin);
        assertThat(saved.getDetails()).isEqualTo("details");
    }

    @Test
    void mappingHandlesNullPerformedBy() {
        AuditLog log = new AuditLog();
        log.setId(1);
        log.setEvent(event);
        log.setAction("X");
        log.setPerformedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmail("admin@e.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByIdAndHermandadId(10, 1)).thenReturn(Optional.of(event));
        when(auditLogRepository.findByEventIdOrderByPerformedAtDesc(any(), any())).thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = service.findByEventId(10, pageable, "admin@e.com");
        assertThat(result.getContent().get(0).performedById()).isNull();
        assertThat(result.getContent().get(0).performedByName()).isNull();
    }

    @Test
    void changeSetBuilder_emptyByDefault() {
        AuditLogService.ChangeSetBuilder cs = new AuditLogService.ChangeSetBuilder();
        assertThat(cs.isEmpty()).isTrue();
        assertThat(cs.toJson()).isNull();
    }

    @Test
    void changeSetBuilder_skipsEqualValues() {
        AuditLogService.ChangeSetBuilder cs = new AuditLogService.ChangeSetBuilder();
        cs.track("name", "a", "a");
        cs.track("nullSame", null, null);
        assertThat(cs.isEmpty()).isTrue();
    }

    @Test
    void changeSetBuilder_tracksDifferences() {
        AuditLogService.ChangeSetBuilder cs = new AuditLogService.ChangeSetBuilder();
        cs.track("name", "old", "new");
        cs.track("count", 1, 2);
        assertThat(cs.isEmpty()).isFalse();
        String json = cs.toJson();
        assertThat(json).contains("\"name\":{\"oldValue\":\"old\",\"newValue\":\"new\"}");
        assertThat(json).contains("\"count\":{\"oldValue\":\"1\",\"newValue\":\"2\"}");
    }

    @Test
    void changeSetBuilder_handlesNullValues() {
        AuditLogService.ChangeSetBuilder cs = new AuditLogService.ChangeSetBuilder();
        cs.track("a", null, "x");
        cs.track("b", "y", null);
        String json = cs.toJson();
        assertThat(json).contains("\"a\":{\"oldValue\":null,\"newValue\":\"x\"}");
        assertThat(json).contains("\"b\":{\"oldValue\":\"y\",\"newValue\":null}");
    }

    @Test
    void changeSetBuilder_escapesSpecialChars() {
        AuditLogService.ChangeSetBuilder cs = new AuditLogService.ChangeSetBuilder();
        cs.track("f", "old", "a\"b\\c\nd\re\tf\u0001g");
        String json = cs.toJson();
        assertThat(json).contains("\\\"").contains("\\\\").contains("\\n").contains("\\r").contains("\\t").contains("\\u0001");
    }

    @Test
    void log_withChanges_persistsChangesField() {
        service.log(event, "EVENT", 10, "UPDATE", admin, "details", "{\"a\":1}");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChanges()).isEqualTo("{\"a\":1}");
    }
}
