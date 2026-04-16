package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HermandadRepository hermandadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       HermandadRepository hermandadRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hermandadRepository = hermandadRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Intento de registro para email: {}", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registro rechazado: email ya registrado [{}]", email);
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }

        RoleCode roleCode = RoleCode.valueOf(request.roleCode());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + request.roleCode()));

        String hermandadNombreTrimmed = request.hermandadNombre().trim();
        Hermandad hermandad;

        if (roleCode == RoleCode.ADMINISTRADOR) {
            if (hermandadRepository.existsByNombreIgnoreCase(hermandadNombreTrimmed)) {
                throw new IllegalStateException("Ya existe una hermandad con ese nombre. Contacta con su administrador para unirte.");
            }
            hermandad = new Hermandad();
            hermandad.setNombre(hermandadNombreTrimmed);
            hermandadRepository.save(hermandad);
            log.info("Nueva hermandad creada: {}", hermandadNombreTrimmed);
        } else {
            hermandad = hermandadRepository.findByNombreIgnoreCase(hermandadNombreTrimmed)
                    .orElseThrow(() -> new IllegalStateException("La hermandad '" + hermandadNombreTrimmed + "' no existe. Contacta con tu administrador."));
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setHermandad(hermandad);

        userRepository.save(user);
        log.info("Usuario registrado correctamente: id={}, email={}, rol={}, hermandad={}", user.getId(), email, roleCode, hermandadNombreTrimmed);

        String token = jwtService.generateToken(user.getEmail());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();

        return new AuthResponse(user.getId(), token, user.getEmail(), user.getFullName(), roles, hermandad.getNombre());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Intento de login para email: {}", email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setLastLogin(LocalDateTime.now());
        log.info("Login exitoso: id={}, email={}", user.getId(), email);

        String token = jwtService.generateToken(user.getEmail());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();

        String hermandadNombre = user.getHermandad() != null ? user.getHermandad().getNombre() : null;
        return new AuthResponse(user.getId(), token, user.getEmail(), user.getFullName(), roles, hermandadNombre);
    }
}
