package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.dto.UserUpdateRequest;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<UserResponse> findAll(String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        return userRepository.findByHermandadId(hermandadId).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        User user = userRepository.findByIdAndHermandadId(id, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado o no pertenece a tu hermandad"));
        return toResponse(user);
    }

    public RoleStatsResponse countByRole(String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        List<User> usersInHermandad = userRepository.findByHermandadId(hermandadId);

        long admins = countUsersWithRole(usersInHermandad, RoleCode.ADMINISTRADOR);
        long responsables = countUsersWithRole(usersInHermandad, RoleCode.RESPONSABLE);
        long colaboradores = countUsersWithRole(usersInHermandad, RoleCode.COLABORADOR);
        long consulta = countUsersWithRole(usersInHermandad, RoleCode.CONSULTA);

        return new RoleStatsResponse(admins, responsables, colaboradores, consulta);
    }

    public UserResponse update(Integer id, UserUpdateRequest request, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        User user = userRepository.findByIdAndHermandadId(id, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado o no pertenece a tu hermandad"));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.roleCode() != null) {
            RoleCode roleCode = RoleCode.valueOf(request.roleCode());
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.roleCode()));
            user.setRoles(new HashSet<>(Set.of(role)));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse toggleActive(Integer id, String authenticatedEmail) {
        Integer hermandadId = resolveHermandadId(authenticatedEmail);
        User user = userRepository.findByIdAndHermandadId(id, hermandadId)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado o no pertenece a tu hermandad"));

        user.setActive(!user.getActive());
        userRepository.save(user);
        return toResponse(user);
    }

    private Integer resolveHermandadId(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authenticatedEmail));
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        return user.getHermandad().getId();
    }

    private long countUsersWithRole(List<User> users, RoleCode roleCode) {
        return users.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode() == roleCode))
                .count();
    }

    private UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roles,
                user.getActive(),
                user.getLastLogin()
        );
    }
}
