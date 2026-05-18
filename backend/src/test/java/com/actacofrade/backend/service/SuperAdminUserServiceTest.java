package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.InterventionLogEntry;
import com.actacofrade.backend.dto.PageResponse;
import com.actacofrade.backend.dto.SuperAdminRoleRequest;
import com.actacofrade.backend.dto.SuperAdminStatusRequest;
import com.actacofrade.backend.dto.SuperAdminUserResponse;
import com.actacofrade.backend.entity.AccountStatus;
import com.actacofrade.backend.entity.AuditLog;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AuditLogRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.service.email.ResendEmailService;
import com.actacofrade.backend.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private ResendEmailService resendEmailService;

    private SuperAdminUserService service;
    private User actor;
    private User target;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        service = new SuperAdminUserService(userRepository, roleRepository, auditLogRepository,
                auditLogService, passwordResetService, resendEmailService);
        actor = TestFixtures.user(100, "super@e.com", null, RoleCode.SUPER_ADMIN);
        target = TestFixtures.user(2, "user@e.com", TestFixtures.hermandad(1, "H"), RoleCode.COLABORADOR);
        superAdmin = TestFixtures.user(3, "other-super@e.com", null, RoleCode.SUPER_ADMIN);
    }

    private void mockActorAndTarget() {
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
    }

    // search

    @Test
    void search_returnsPagedResults() {
        when(userRepository.searchAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(target)));
        PageResponse<SuperAdminUserResponse> resp = service.search("foo", 0, 10);
        assertThat(resp.content()).hasSize(1);
        assertThat(resp.content().get(0).id()).isEqualTo(2);
    }

    @Test
    void search_clampsNegativePageAndSize() {
        when(userRepository.searchAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(target)));
        assertThat(service.search(null, -1, 0).content()).hasSize(1);
        assertThat(service.search(null, 0, 200).content()).hasSize(1);
    }

    // findById

    @Test
    void findById_returnsResponse() {
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        SuperAdminUserResponse r = service.findById(2);
        assertThat(r.email()).isEqualTo("user@e.com");
        assertThat(r.status()).isEqualTo("ACTIVE");
    }

    @Test
    void findById_missing_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99)).isInstanceOf(IllegalArgumentException.class);
    }

    // updateStatus

    @Test
    void updateStatus_suspend_persistsAndSendsEmail() {
        mockActorAndTarget();
        when(resendEmailService.sendAccountStatusEmail(any(), any(), any(), any())).thenReturn(true);

        SuperAdminUserResponse r = service.updateStatus(2,
                new SuperAdminStatusRequest("SUSPENDED", "incumplimiento"), actor.getEmail());

        assertThat(r.status()).isEqualTo("SUSPENDED");
        assertThat(target.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(target.getActive()).isFalse();
        verify(userRepository).save(target);
        verify(auditLogService).log(any(), anyString(), anyInt(), anyString(), any(), anyString(), any());
    }

    @Test
    void updateStatus_ban_emailFailureStillLogs() {
        mockActorAndTarget();
        when(resendEmailService.sendAccountStatusEmail(any(), any(), any(), any())).thenReturn(false);
        service.updateStatus(2, new SuperAdminStatusRequest("BANNED", "spam"), actor.getEmail());
        assertThat(target.getStatus()).isEqualTo(AccountStatus.BANNED);
    }

    @Test
    void updateStatus_activate_clearsReason() {
        target.setStatus(AccountStatus.SUSPENDED);
        target.setStatusReason("antiguo");
        mockActorAndTarget();
        service.updateStatus(2, new SuperAdminStatusRequest("ACTIVE", null), actor.getEmail());
        assertThat(target.getStatusReason()).isNull();
        assertThat(target.getActive()).isTrue();
    }

    @Test
    void updateStatus_suspendWithoutReason_throws() {
        mockActorAndTarget();
        assertThatThrownBy(() -> service.updateStatus(2,
                new SuperAdminStatusRequest("SUSPENDED", " "), actor.getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_onSuperAdminTarget_throws() {
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(userRepository.findById(3)).thenReturn(Optional.of(superAdmin));
        assertThatThrownBy(() -> service.updateStatus(3,
                new SuperAdminStatusRequest("ACTIVE", null), actor.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_onSelf_throws() {
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        // self target with a non super-admin role so it passes rejectIfSuperAdmin
        User self = TestFixtures.user(100, actor.getEmail(), null, RoleCode.COLABORADOR);
        when(userRepository.findById(100)).thenReturn(Optional.of(self));
        assertThatThrownBy(() -> service.updateStatus(100,
                new SuperAdminStatusRequest("SUSPENDED", "x"), actor.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_actorNotFound_throws() {
        when(userRepository.findByEmail("nope@e.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(2,
                new SuperAdminStatusRequest("ACTIVE", null), "nope@e.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // setManualVerification

    @Test
    void setManualVerification_verify_setsFlagAndActor() {
        mockActorAndTarget();
        SuperAdminUserResponse r = service.setManualVerification(2, true, actor.getEmail());
        assertThat(r.manuallyVerified()).isTrue();
        assertThat(target.getManuallyVerifiedBy()).isSameAs(actor);
        assertThat(target.getManuallyVerifiedAt()).isNotNull();
    }

    @Test
    void setManualVerification_unverify_clearsFlag() {
        target.setManuallyVerified(true);
        target.setManuallyVerifiedBy(actor);
        target.setManuallyVerifiedAt(LocalDateTime.now());
        mockActorAndTarget();
        service.setManualVerification(2, false, actor.getEmail());
        assertThat(target.getManuallyVerified()).isFalse();
        assertThat(target.getManuallyVerifiedAt()).isNull();
        assertThat(target.getManuallyVerifiedBy()).isNull();
    }

    // overrideRole

    @Test
    void overrideRole_replacesRoles() {
        mockActorAndTarget();
        Role respRole = TestFixtures.role(7, RoleCode.RESPONSABLE);
        when(roleRepository.findByCode(RoleCode.RESPONSABLE)).thenReturn(Optional.of(respRole));
        SuperAdminUserResponse r = service.overrideRole(2,
                new SuperAdminRoleRequest("RESPONSABLE", "promoción"), actor.getEmail());
        assertThat(r.roles()).containsExactly("RESPONSABLE");
    }

    @Test
    void overrideRole_toSuperAdmin_throws() {
        mockActorAndTarget();
        assertThatThrownBy(() -> service.overrideRole(2,
                new SuperAdminRoleRequest("SUPER_ADMIN", "x"), actor.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void overrideRole_blankReason_throws() {
        mockActorAndTarget();
        assertThatThrownBy(() -> service.overrideRole(2,
                new SuperAdminRoleRequest("RESPONSABLE", " "), actor.getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overrideRole_roleNotFound_throws() {
        mockActorAndTarget();
        when(roleRepository.findByCode(RoleCode.RESPONSABLE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overrideRole(2,
                new SuperAdminRoleRequest("RESPONSABLE", "motivo"), actor.getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // triggerPasswordReset

    @Test
    void triggerPasswordReset_success() {
        mockActorAndTarget();
        when(passwordResetService.issueTokenFor(target, actor)).thenReturn("secret");
        when(passwordResetService.expirationMinutes()).thenReturn(60L);
        when(resendEmailService.sendPasswordResetEmail(any(), any(), any(), any(Long.class))).thenReturn(true);
        service.triggerPasswordReset(2, actor.getEmail());
        verify(auditLogService).log(any(), anyString(), anyInt(), anyString(), any(), anyString(), any());
    }

    @Test
    void triggerPasswordReset_emailFails_throwsIllegalState() {
        mockActorAndTarget();
        when(passwordResetService.issueTokenFor(target, actor)).thenReturn("secret");
        when(passwordResetService.expirationMinutes()).thenReturn(60L);
        when(resendEmailService.sendPasswordResetEmail(any(), any(), any(), any(Long.class))).thenReturn(false);
        assertThatThrownBy(() -> service.triggerPasswordReset(2, actor.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void triggerPasswordReset_bannedTarget_throws() {
        target.setStatus(AccountStatus.BANNED);
        mockActorAndTarget();
        assertThatThrownBy(() -> service.triggerPasswordReset(2, actor.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    // logs

    @Test
    void findUserLogs_mapsAuditEntries() {
        AuditLog entry = new AuditLog();
        entry.setId(1);
        entry.setAction("SUPERADMIN_STATUS_CHANGE");
        entry.setEntityId(2);
        entry.setPerformedBy(actor);
        entry.setPerformedAt(LocalDateTime.now());
        entry.setDetails("d");
        entry.setChanges("{}");
        when(auditLogRepository.findByEntityTypeAndEntityIdAndActionStartingWithOrderByPerformedAtDesc(
                any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        PageResponse<InterventionLogEntry> page = service.findUserLogs(2, 0, 20);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).action()).isEqualTo("SUPERADMIN_STATUS_CHANGE");
    }

    @Test
    void findAllLogs_handlesNullPerformer() {
        AuditLog entry = new AuditLog();
        entry.setId(2);
        entry.setAction("SUPERADMIN_MANUAL_VERIFY");
        entry.setEntityId(2);
        entry.setPerformedAt(LocalDateTime.now());
        when(auditLogRepository.findByEntityTypeAndActionStartingWithOrderByPerformedAtDesc(
                any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        PageResponse<InterventionLogEntry> page = service.findAllLogs(-1, -1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).actorId()).isNull();
    }
}
