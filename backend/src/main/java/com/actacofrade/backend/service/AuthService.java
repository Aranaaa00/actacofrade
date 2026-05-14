package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.AuthResponse;
import com.actacofrade.backend.dto.HermandadOption;
import com.actacofrade.backend.dto.LoginRequest;
import com.actacofrade.backend.dto.RegisterRequest;
import com.actacofrade.backend.dto.RegistrationStatusResponse;
import com.actacofrade.backend.entity.AccountStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.exception.AccountBannedException;
import com.actacofrade.backend.exception.AccountSuspendedException;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserAvatarRepository;
import com.actacofrade.backend.repository.UserRepository;
import com.actacofrade.backend.security.JwtService;
import com.actacofrade.backend.service.email.ResendEmailService;
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
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HermandadRepository hermandadRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final ResendEmailService resendEmailService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       HermandadRepository hermandadRepository,
                       UserAvatarRepository userAvatarRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       PendingRegistrationStore pendingRegistrationStore,
                       ResendEmailService resendEmailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hermandadRepository = hermandadRepository;
        this.userAvatarRepository = userAvatarRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.pendingRegistrationStore = pendingRegistrationStore;
        this.resendEmailService = resendEmailService;
    }

    /**
     * Inicia el flujo de registro: valida los datos, guarda una entrada
     * temporal en memoria y envía el correo de verificación. NO crea
     * ningún usuario en base de datos hasta que se confirme el correo.
     */
    public RegistrationStatusResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Solicitud de verificación de correo para: {}", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Solicitud de registro rechazada: email ya registrado [{}]", email);
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }

        RoleCode roleCode;
        try {
            roleCode = RoleCode.valueOf(request.roleCode());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol no válido: " + request.roleCode());
        }

        String hermandadNombreTrimmed = request.hermandadNombre().trim();
        if (roleCode == RoleCode.ADMINISTRADOR) {
            if (hermandadRepository.existsByNombreIgnoreCase(hermandadNombreTrimmed)) {
                throw new IllegalStateException("Ya existe una hermandad con ese nombre. Contacta con su administrador para unirte.");
            }
        } else {
            if (hermandadRepository.findByNombreIgnoreCase(hermandadNombreTrimmed).isEmpty()) {
                throw new IllegalStateException("La hermandad '" + hermandadNombreTrimmed + "' no existe. Contacta con tu administrador.");
            }
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        String token = pendingRegistrationStore.create(request, encodedPassword);

        boolean delivered = resendEmailService.sendVerificationEmail(
                email,
                request.fullName().trim(),
                token,
                pendingRegistrationStore.expirationMinutes()
        );

        if (!delivered) {
            pendingRegistrationStore.invalidateByEmail(email);
            throw new IllegalStateException("No se pudo enviar el correo de verificación. Inténtalo de nuevo en unos minutos.");
        }

        log.info("Correo de verificación enviado a {}", email);
        return RegistrationStatusResponse.pendingVerification();
    }

    /**
     * Reenvía el correo de verificación si existe una solicitud pendiente.
     * Respuesta genérica para no revelar si el email tiene o no una solicitud.
     */
    public RegistrationStatusResponse resendVerification(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        Optional<PendingRegistrationStore.PendingRegistration> existing = pendingRegistrationStore.findByEmail(email);
        if (existing.isPresent()) {
            PendingRegistrationStore.PendingRegistration data = existing.get();
            RegisterRequest copy = new RegisterRequest(
                    data.fullName(), data.email(), "PlaceholderAa1@x",
                    data.roleCode(), data.hermandadNombre()
            );
            String token = pendingRegistrationStore.create(copy, data.encodedPassword());
            resendEmailService.sendVerificationEmail(
                    data.email(), data.fullName(), token, pendingRegistrationStore.expirationMinutes());
            log.info("Reenvío de verificación procesado para {}", email);
        } else {
            log.debug("Reenvío solicitado para email sin pendiente activo (respuesta genérica)");
        }
        return RegistrationStatusResponse.pendingVerification();
    }

    /**
     * Consume el token de verificación: si es válido, crea el usuario en
     * base de datos, le asigna su rol y hermandad y devuelve la sesión JWT.
     */
    @Transactional
    public AuthResponse verifyEmail(String token) {
        Optional<PendingRegistrationStore.PendingRegistration> consumed = pendingRegistrationStore.consume(token);
        if (consumed.isEmpty()) {
            throw new IllegalArgumentException("Enlace de verificación no válido o caducado");
        }
        PendingRegistrationStore.PendingRegistration pending = consumed.get();

        if (userRepository.existsByEmail(pending.email())) {
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }

        RoleCode roleCode = RoleCode.valueOf(pending.roleCode());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + pending.roleCode()));

        Hermandad hermandad;
        if (roleCode == RoleCode.ADMINISTRADOR) {
            if (hermandadRepository.existsByNombreIgnoreCase(pending.hermandadNombre())) {
                throw new IllegalStateException("Ya existe una hermandad con ese nombre. Contacta con su administrador para unirte.");
            }
            hermandad = new Hermandad();
            hermandad.setNombre(pending.hermandadNombre());
            hermandadRepository.save(hermandad);
            log.info("Nueva hermandad creada tras verificación: {}", pending.hermandadNombre());
        } else {
            hermandad = hermandadRepository.findByNombreIgnoreCase(pending.hermandadNombre())
                    .orElseThrow(() -> new IllegalStateException("La hermandad '" + pending.hermandadNombre() + "' ya no existe."));
        }

        User user = new User();
        user.setFullName(pending.fullName());
        user.setEmail(pending.email());
        user.setPasswordHash(pending.encodedPassword());
        user.setActive(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setHermandad(hermandad);
        userRepository.save(user);

        log.info("Usuario verificado y registrado: id={}, email={}, rol={}",
                user.getId(), user.getEmail(), roleCode);

        String jwt = jwtService.generateToken(user.getEmail());
        List<String> roles = user.getRoles().stream().map(r -> r.getCode().name()).toList();
        return new AuthResponse(user.getId(), jwt, user.getEmail(), user.getFullName(), roles,
                hermandad.getNombre(), userAvatarRepository.existsByUserId(user.getId()),
                Boolean.TRUE.equals(user.getManuallyVerified()));
    }

    @Transactional(readOnly = true)
    public List<HermandadOption> listHermandades() {
        return hermandadRepository.findAllByOrderByNombreAsc().stream()
                .map(h -> new HermandadOption(h.getId(), h.getNombre()))
                .toList();
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

        enforceAccountStatusOrFail(user);

        user.setLastLogin(LocalDateTime.now());
        log.info("Login exitoso: id={}, email={}", user.getId(), email);

        String token = jwtService.generateToken(user.getEmail());
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getCode().name())
                .toList();

        String hermandadNombre = user.getHermandad() != null ? user.getHermandad().getNombre() : null;
        return new AuthResponse(user.getId(), token, user.getEmail(), user.getFullName(), roles, hermandadNombre,
                userAvatarRepository.existsByUserId(user.getId()),
                Boolean.TRUE.equals(user.getManuallyVerified()));
    }

    /**
     * Verifica el estado de la cuenta tras autenticar las credenciales.
     * Solo se invoca con contraseña correcta para no filtrar el estado a atacantes.
     */
    private void enforceAccountStatusOrFail(User user) {
        AccountStatus status = user.getStatus();
        if (status == null || status == AccountStatus.ACTIVE) {
            return;
        }
        log.warn("Acceso denegado por estado de cuenta: id={}, status={}", user.getId(), status);
        if (status == AccountStatus.BANNED) {
            throw new AccountBannedException(user.getStatusReason());
        }
        throw new AccountSuspendedException(user.getStatusReason());
    }
}