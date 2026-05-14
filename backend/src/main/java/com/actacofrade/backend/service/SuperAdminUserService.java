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
import com.actacofrade.backend.util.SanitizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Centro de Intervención del SuperAdmin: cambios de estado de cuenta, verificación manual, override de rol y disparo de reset de contraseña.
 * Todos los métodos persisten una entrada en {@link AuditLog} reutilizando el servicio existente para no duplicar la infraestructura de auditoría.
 */
@Service
@Transactional
public class SuperAdminUserService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final PasswordResetService passwordResetService;
    private final ResendEmailService resendEmailService;

    public SuperAdminUserService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 AuditLogRepository auditLogRepository,
                                 AuditLogService auditLogService,
                                 PasswordResetService passwordResetService,
                                 ResendEmailService resendEmailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.passwordResetService = passwordResetService;
        this.resendEmailService = resendEmailService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SuperAdminUserResponse> search(String query, int page, int size) {
        String sanitizedQuery = SanitizationUtils.sanitizeNullable(query);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchAll(sanitizedQuery, pageable);
        List<SuperAdminUserResponse> mapped = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(mapped, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SuperAdminUserResponse findById(Integer userId) {
        return toResponse(loadTarget(userId));
    }

    public SuperAdminUserResponse updateStatus(Integer userId, SuperAdminStatusRequest request, String actorEmail) {
        User actor = loadActor(actorEmail);
        User target = loadTarget(userId);
        rejectIfSuperAdmin(target);
        rejectIfSelf(actor, target);

        AccountStatus newStatus = AccountStatus.valueOf(request.status());
        AccountStatus oldStatus = target.getStatus();
        if (newStatus != AccountStatus.ACTIVE && (request.reason() == null || request.reason().isBlank())) {
            throw new IllegalArgumentException("Debes indicar un motivo para suspender o bloquear la cuenta");
        }
        String reason = SanitizationUtils.sanitizeNullable(request.reason());

        target.setStatus(newStatus);
        target.setStatusReason(newStatus == AccountStatus.ACTIVE ? null : reason);
        target.setStatusChangedAt(LocalDateTime.now());
        target.setStatusChangedBy(actor);
        target.setActive(newStatus == AccountStatus.ACTIVE);
        userRepository.save(target);

        String changes = new AuditLogService.ChangeSetBuilder()
                .track("status", oldStatus, newStatus)
                .track("reason", null, reason)
                .toJson();
        boolean emailSent = false;
        if (newStatus == AccountStatus.SUSPENDED || newStatus == AccountStatus.BANNED) {
            emailSent = resendEmailService.sendAccountStatusEmail(
                    target.getEmail(), target.getFullName(), newStatus, reason);
        }
        String details = switch (newStatus) {
            case SUSPENDED -> emailSent ? "Cuenta suspendida y notificación enviada" : "Cuenta suspendida (notificación NO enviada)";
            case BANNED -> emailSent ? "Cuenta bloqueada y notificación enviada" : "Cuenta bloqueada (notificación NO enviada)";
            case ACTIVE -> "Cuenta reactivada";
        };
        auditLogService.log(null, SuperAdminAuditActions.ENTITY_TYPE_USER, target.getId(),
                SuperAdminAuditActions.ACTION_STATUS_CHANGE, actor, details, changes);

        log.info("SuperAdmin {} cambió estado de userId={} de {} a {}",
                actor.getId(), target.getId(), oldStatus, newStatus);
        return toResponse(target);
    }

    public SuperAdminUserResponse setManualVerification(Integer userId,
                                                       boolean verified,
                                                       String actorEmail) {
        User actor = loadActor(actorEmail);
        User target = loadTarget(userId);
        rejectIfSuperAdmin(target);

        boolean previous = Boolean.TRUE.equals(target.getManuallyVerified());
        target.setManuallyVerified(verified);
        target.setManuallyVerifiedAt(verified ? LocalDateTime.now() : null);
        target.setManuallyVerifiedBy(verified ? actor : null);
        userRepository.save(target);

        String changes = new AuditLogService.ChangeSetBuilder()
                .track("manuallyVerified", previous, verified)
                .toJson();
        String action = verified
                ? SuperAdminAuditActions.ACTION_MANUAL_VERIFY
                : SuperAdminAuditActions.ACTION_MANUAL_UNVERIFY;
        auditLogService.log(null, SuperAdminAuditActions.ENTITY_TYPE_USER, target.getId(),
                action, actor, verified ? "Verificación manual concedida" : "Verificación manual retirada",
                changes);
        return toResponse(target);
    }

    public SuperAdminUserResponse overrideRole(Integer userId, SuperAdminRoleRequest request, String actorEmail) {
        User actor = loadActor(actorEmail);
        User target = loadTarget(userId);
        rejectIfSuperAdmin(target);

        RoleCode newRoleCode = RoleCode.valueOf(request.roleCode());
        if (newRoleCode == RoleCode.SUPER_ADMIN) {
            throw new AccessDeniedException("No se puede asignar SUPER_ADMIN manualmente");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Debes indicar un motivo para el cambio de rol");
        }

        Role newRole = roleRepository.findByCode(newRoleCode)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + newRoleCode));
        String previousRoles = target.getRoles().stream()
                .map(r -> r.getCode().name())
                .sorted()
                .collect(java.util.stream.Collectors.joining(","));

        target.setRoles(new HashSet<>(Set.of(newRole)));
        userRepository.save(target);

        String reason = SanitizationUtils.sanitizeNullable(request.reason());
        String changes = new AuditLogService.ChangeSetBuilder()
                .track("roles", previousRoles, newRoleCode.name())
                .track("reason", null, reason)
                .toJson();
        auditLogService.log(null, SuperAdminAuditActions.ENTITY_TYPE_USER, target.getId(),
                SuperAdminAuditActions.ACTION_ROLE_OVERRIDE, actor,
                "Override manual de rol", changes);

        log.info("SuperAdmin {} sobrescribió rol de userId={} a {}", actor.getId(), target.getId(), newRoleCode);
        return toResponse(target);
    }

    /**
     * Emite el token y envía el correo. Si el envío falla, dejamos commiteada la
     * entrada de auditoría con el fallo y propagamos un error 500 al cliente. El token queda emitido pero caducará en su plazo natural sin
     * dejar huérfana la traza.
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public void triggerPasswordReset(Integer userId, String actorEmail) {
        User actor = loadActor(actorEmail);
        User target = loadTarget(userId);
        rejectIfSuperAdmin(target);
        if (target.getStatus() == AccountStatus.BANNED) {
            throw new IllegalStateException("No se puede restablecer la contraseña de una cuenta bloqueada");
        }

        String secret = passwordResetService.issueTokenFor(target, actor);
        boolean delivered = resendEmailService.sendPasswordResetEmail(
                target.getEmail(), target.getFullName(), secret, passwordResetService.expirationMinutes());

        auditLogService.log(null, SuperAdminAuditActions.ENTITY_TYPE_USER, target.getId(),
                SuperAdminAuditActions.ACTION_PASSWORD_RESET_TRIGGERED, actor,
                delivered ? "Envío correcto del enlace de reset" : "Fallo en el envío del correo de reset",
                null);

        if (!delivered) {
            throw new IllegalStateException("No se pudo enviar el correo de restablecimiento. Inténtalo de nuevo.");
        }
        log.info("SuperAdmin {} disparó reset de contraseña para userId={}", actor.getId(), target.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<InterventionLogEntry> findUserLogs(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<AuditLog> result = auditLogRepository
                .findByEntityTypeAndEntityIdAndActionStartingWithOrderByPerformedAtDesc(
                        SuperAdminAuditActions.ENTITY_TYPE_USER, userId, "SUPERADMIN_", pageable);
        return toLogPage(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<InterventionLogEntry> findAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<AuditLog> result = auditLogRepository
                .findByEntityTypeAndActionStartingWithOrderByPerformedAtDesc(
                        SuperAdminAuditActions.ENTITY_TYPE_USER, "SUPERADMIN_", pageable);
        return toLogPage(result);
    }

    private PageResponse<InterventionLogEntry> toLogPage(Page<AuditLog> result) {
        List<InterventionLogEntry> mapped = result.getContent().stream()
                .map(this::toLogEntry)
                .toList();
        return new PageResponse<>(mapped, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private InterventionLogEntry toLogEntry(AuditLog entry) {
        Integer actorId = entry.getPerformedBy() == null ? null : entry.getPerformedBy().getId();
        String actorName = entry.getPerformedBy() == null ? null : entry.getPerformedBy().getFullName();
        return new InterventionLogEntry(entry.getId(), entry.getAction(), entry.getEntityId(),
                actorId, actorName, entry.getPerformedAt(), entry.getDetails(), entry.getChanges());
    }

    private SuperAdminUserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();
        return new SuperAdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roles,
                user.getStatus() == null ? AccountStatus.ACTIVE.name() : user.getStatus().name(),
                user.getStatusReason(),
                user.getStatusChangedAt(),
                Boolean.TRUE.equals(user.getManuallyVerified()),
                user.getManuallyVerifiedAt(),
                user.getHermandad() == null ? null : user.getHermandad().getNombre(),
                user.getLastLogin(),
                user.getCreatedAt()
        );
    }

    private User loadActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Sesión no válida"));
    }

    private User loadTarget(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    private void rejectIfSuperAdmin(User target) {
        boolean isSuper = target.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN);
        if (isSuper) {
            throw new AccessDeniedException("No se puede operar sobre la cuenta del SuperAdmin");
        }
    }

    private void rejectIfSelf(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            throw new AccessDeniedException("No puedes operar sobre tu propia cuenta");
        }
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
