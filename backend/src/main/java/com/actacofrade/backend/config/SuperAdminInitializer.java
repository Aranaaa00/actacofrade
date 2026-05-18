package com.actacofrade.backend.config;

import com.actacofrade.backend.entity.Role;
import com.actacofrade.backend.entity.RoleCode;
import com.actacofrade.backend.entity.User;
import com.actacofrade.backend.repository.RoleRepository;
import com.actacofrade.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Create the super admin user on first startup if it does not exist yet.
 * Credentials come only from environment variables; without them this runner is skipped.
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${superadmin.email:}")
    private String email;

    @Value("${superadmin.password:}")
    private String password;

    @Value("${superadmin.full-name:}")
    private String fullName;

    public SuperAdminInitializer(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPassword = StringUtils.hasText(password);
        boolean hasFullName = StringUtils.hasText(fullName);

        // All-or-nothing: refuse to start with a half-configured super admin to
        // avoid silently leaving the platform without a working SUPER_ADMIN.
        if (!hasEmail && !hasPassword && !hasFullName) {
            log.warn("Variables de super administrador no configuradas. Inicializacion omitida.");
            return;
        }
        if (!(hasEmail && hasPassword && hasFullName)) {
            throw new IllegalStateException(
                    "Configuracion incompleta del super administrador: SUPERADMIN_EMAIL, "
                            + "SUPERADMIN_PASSWORD y SUPERADMIN_FULL_NAME deben definirse en conjunto.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        Role superAdminRole = roleRepository.findByCode(RoleCode.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rol SUPER_ADMIN no configurado"));

        User superAdmin = new User();
        superAdmin.setEmail(email);
        superAdmin.setFullName(fullName);
        superAdmin.setPasswordHash(passwordEncoder.encode(password));
        superAdmin.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(superAdminRole);
        superAdmin.setRoles(roles);
        userRepository.save(superAdmin);

        log.info("Usuario super administrador creado");
    }
}
