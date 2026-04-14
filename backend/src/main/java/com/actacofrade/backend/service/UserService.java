package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.dto.UserUpdateRequest;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
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

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toResponse(user);
    }

    public RoleStatsResponse countByRole() {
        List<User> allUsers = userRepository.findAll();

        long admins = countUsersWithRole(allUsers, RoleCode.ADMINISTRADOR);
        long responsables = countUsersWithRole(allUsers, RoleCode.RESPONSABLE);
        long colaboradores = countUsersWithRole(allUsers, RoleCode.COLABORADOR);
        long consulta = countUsersWithRole(allUsers, RoleCode.CONSULTA);

        return new RoleStatsResponse(admins, responsables, colaboradores, consulta);
    }

    public UserResponse update(Integer id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

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

    public UserResponse toggleActive(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        user.setActive(!user.getActive());
        userRepository.save(user);
        return toResponse(user);
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
