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

import java.util.HashSet;
import java.util.Set;

/**
 * Crea el usuario super administrador en el primer arranque si no existe.
 * Las credenciales se cargan desde variables de entorno y, en su defecto,
 * caen sobre valores de desarrollo. La contraseña debe rotarse en produccion.
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${superadmin.email:superadmin@actacofrade.local}")
    private String email;

    @Value("${superadmin.password:SuperAdmin1!}")
    private String password;

    @Value("${superadmin.full-name:Super Administrador}")
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

        log.info("Usuario super administrador creado: {}", email);
    }
}
