package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.RoleStatsResponse;
import com.actacofrade.backend.dto.UserResponse;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
