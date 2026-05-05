package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AdminChangeRequestApprove;
import com.actacofrade.backend.dto.AdminChangeRequestCreate;
import com.actacofrade.backend.dto.AdminChangeRequestResponse;
import com.actacofrade.backend.entity.AdminChangeRequest;
import com.actacofrade.backend.entity.AdminChangeRequestStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.AdminChangeRequestRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.util.SanitizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdminChangeRequestService {

    private static final Logger log = LoggerFactory.getLogger(AdminChangeRequestService.class);

    private final AdminChangeRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminChangeRequestService(AdminChangeRequestRepository requestRepository,
                                     UserRepository userRepository,
                                     RoleRepository roleRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public AdminChangeRequestResponse create(AdminChangeRequestCreate request, String authenticatedEmail) {
        User requester = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authenticatedEmail));
        Hermandad hermandad = requester.getHermandad();
        if (hermandad == null) {
            throw new IllegalStateException("Solo los miembros de una hermandad pueden enviar solicitudes");
        }
        if (hasSuperAdminRole(requester)) {
            throw new AccessDeniedException("El super administrador no puede enviar solicitudes");
        }

        AdminChangeRequest entity = new AdminChangeRequest();
        entity.setHermandad(hermandad);
        entity.setRequester(requester);
        entity.setMessage(SanitizationUtils.sanitize(request.message()));
        entity.setStatus(AdminChangeRequestStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());

        requestRepository.save(entity);
        log.info("Solicitud de cambio de admin creada id={} hermandad={} solicitante={}",
                entity.getId(), hermandad.getId(), requester.getEmail());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AdminChangeRequestResponse> findAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminChangeRequestResponse findById(Integer id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<com.actacofrade.backend.dto.UserResponse> findCandidates(Integer requestId) {
        AdminChangeRequest request = getOrThrow(requestId);
        Integer hermandadId = request.getHermandad().getId();
        return userRepository.findByHermandadId(hermandadId).stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getRoles().stream().noneMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN))
                .map(u -> new com.actacofrade.backend.dto.UserResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRoles().stream().map(r -> r.getCode().name()).toList(),
                        u.getActive(),
                        u.getLastLogin(),
                        false
                ))
                .toList();
    }

    public AdminChangeRequestResponse approve(Integer id, AdminChangeRequestApprove payload, String authenticatedEmail) {
        AdminChangeRequest request = getOrThrow(id);
        ensurePending(request);

        User newAdmin = userRepository.findById(payload.newAdminUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario destinatario no encontrado"));
        if (newAdmin.getHermandad() == null
                || !newAdmin.getHermandad().getId().equals(request.getHermandad().getId())) {
            throw new IllegalStateException("El nuevo administrador debe pertenecer a la misma hermandad");
        }
        if (Boolean.FALSE.equals(newAdmin.getActive())) {
            throw new IllegalStateException("El nuevo administrador debe estar activo");
        }
        if (hasSuperAdminRole(newAdmin)) {
            throw new IllegalStateException("No se puede asignar el rol al super administrador");
        }

        Role adminRole = roleRepository.findByCode(RoleCode.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("Rol ADMINISTRADOR no configurado"));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        newAdmin.setRoles(roles);
        userRepository.save(newAdmin);

        User resolver = userRepository.findByEmail(authenticatedEmail).orElse(null);
        request.setStatus(AdminChangeRequestStatus.APPROVED);
        request.setNewAdmin(newAdmin);
        request.setResolvedBy(resolver);
        request.setResolvedAt(LocalDateTime.now());

        log.warn("Solicitud {} APROBADA por={} nuevoAdmin={} hermandad={}",
                id, authenticatedEmail, newAdmin.getEmail(), request.getHermandad().getId());
        return toResponse(request);
    }

    public AdminChangeRequestResponse reject(Integer id, String authenticatedEmail) {
        AdminChangeRequest request = getOrThrow(id);
        ensurePending(request);

        User resolver = userRepository.findByEmail(authenticatedEmail).orElse(null);
        request.setStatus(AdminChangeRequestStatus.REJECTED);
        request.setResolvedBy(resolver);
        request.setResolvedAt(LocalDateTime.now());

        log.warn("Solicitud {} RECHAZADA por={} hermandad={}",
                id, authenticatedEmail, request.getHermandad().getId());
        return toResponse(request);
    }

    private AdminChangeRequest getOrThrow(Integer id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
    }

    private void ensurePending(AdminChangeRequest request) {
        if (request.getStatus() != AdminChangeRequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya ha sido resuelta");
        }
    }

    private boolean hasSuperAdminRole(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN);
    }

    private AdminChangeRequestResponse toResponse(AdminChangeRequest entity) {
        Hermandad hermandad = entity.getHermandad();
        User requester = entity.getRequester();
        User newAdmin = entity.getNewAdmin();
        User resolver = entity.getResolvedBy();
        return new AdminChangeRequestResponse(
                entity.getId(),
                hermandad != null ? hermandad.getId() : null,
                hermandad != null ? hermandad.getNombre() : null,
                requester != null ? requester.getId() : null,
                requester != null ? requester.getFullName() : null,
                requester != null ? requester.getEmail() : null,
                entity.getMessage(),
                entity.getStatus().name(),
                newAdmin != null ? newAdmin.getId() : null,
                newAdmin != null ? newAdmin.getFullName() : null,
                resolver != null ? resolver.getId() : null,
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }
}
