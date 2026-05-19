package com.actacofrade.backend.config;

import com.actacofrade.backend.entity.AccountStatus;
import com.actacofrade.backend.entity.Hermandad;
import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.HermandadRepository;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Inicializador de datos de prueba para entorno local. Solo se ejecuta cuando la propiedad
 * app.seed.test-users es true (NUNCA debe activarse en producción). Crea una hermandad de
 * prueba y un usuario por cada rol no privilegiado (ADMINISTRADOR, RESPONSABLE, COLABORADOR
 * verificado y sin verificar, CONSULTA) cubriendo todos los flujos del Centro de Intervención.
 * Los usuarios sembrados comparten una contraseña conocida documentada en README de despliegue.
 */
@Component
@Order(20)
public class TestUsersInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestUsersInitializer.class);

    private static final String TEST_HERMANDAD = "Hermandad de Pruebas";
    private static final String DEFAULT_PASSWORD = "Test1234!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HermandadRepository hermandadRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.seed.test-users:false}")
    private boolean enabled;

    @Value("${app.seed.test-users.password:" + DEFAULT_PASSWORD + "}")
    private String seedPassword;

    public TestUsersInitializer(UserRepository userRepository,
                                RoleRepository roleRepository,
                                HermandadRepository hermandadRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hermandadRepository = hermandadRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        // Hard guard: even if the operator sets app.seed.test-users=true by mistake,
        // we refuse to seed shared accounts when the prod profile is active.
        for (String active : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(active) || "production".equalsIgnoreCase(active)) {
                log.warn("Seed de usuarios de prueba IGNORADO: perfil de produccion activo ({}).", active);
                return;
            }
        }
        Hermandad hermandad = hermandadRepository.findByNombreIgnoreCase(TEST_HERMANDAD)
                .orElseGet(() -> {
                    Hermandad nueva = new Hermandad();
                    nueva.setNombre(TEST_HERMANDAD);
                    return hermandadRepository.save(nueva);
                });

        List<SeedUser> seeds = List.of(
                new SeedUser("admin.test@actacofrade.local", "Admin Pruebas", RoleCode.ADMINISTRADOR, false),
                new SeedUser("responsable.test@actacofrade.local", "Responsable Pruebas", RoleCode.RESPONSABLE, false),
                new SeedUser("colab1.test@actacofrade.local", "Colaborador Uno", RoleCode.COLABORADOR, false),
                new SeedUser("colab2.test@actacofrade.local", "Colaborador Dos", RoleCode.COLABORADOR, false),
                new SeedUser("consulta.test@actacofrade.local", "Consulta Pruebas", RoleCode.CONSULTA, false)
        );

        for (SeedUser seed : seeds) {
            if (userRepository.findByEmail(seed.email).isPresent()) {
                continue;
            }
            Role role = roleRepository.findByCode(seed.roleCode)
                    .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + seed.roleCode));
            User user = new User();
            user.setEmail(seed.email);
            user.setFullName(seed.fullName);
            user.setPasswordHash(passwordEncoder.encode(seedPassword));
            user.setActive(true);
            user.setStatus(AccountStatus.ACTIVE);
            user.setRoles(new HashSet<>(Set.of(role)));
            user.setHermandad(hermandad);
            if (seed.manuallyVerified) {
                user.setManuallyVerified(true);
                user.setManuallyVerifiedAt(LocalDateTime.now());
            }
            userRepository.save(user);
            log.info("Usuario de prueba creado: {} ({})", seed.email, seed.roleCode);
        }
    }

    private record SeedUser(String email, String fullName, RoleCode roleCode, boolean manuallyVerified) {
    }
}
