package com.shopmart.config;

import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Optionally seeds staff accounts on startup.
 *
 * <p>No password has a default. An account is created only when its password is supplied
 * through the environment (ADMIN_PASSWORD, SUPERADMIN_PASSWORD, ENGINEER_PASSWORD).
 * Existing users are never modified, so this is safe to leave enabled in production.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@shopmart.local}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.superadmin.email:superadmin@shopmart.local}")
    private String superAdminEmail;

    @Value("${app.superadmin.password:}")
    private String superAdminPassword;

    @Value("${app.engineer.email:engineer@shopmart.local}")
    private String engineerEmail;

    @Value("${app.engineer.password:}")
    private String engineerPassword;

    @Override
    public void run(String... args) {
        log.info("Starting optional staff account initialization...");

        seedAdmin();
        seedSuperAdmin();
        seedEngineer();

        log.info("Staff account initialization completed.");
    }

    private void seedAdmin() {
        createUserIfConfigured(
                adminEmail,
                adminPassword,
                "ShopMart Admin",
                Role.ROLE_ADMIN,
                Role.ROLE_CUSTOMER
        );
    }

    private void seedSuperAdmin() {
        createUserIfConfigured(
                superAdminEmail,
                superAdminPassword,
                "Super Admin",
                Role.ROLE_SUPER_ADMIN,
                Role.ROLE_ADMIN,
                Role.ROLE_CUSTOMER
        );
    }

    private void seedEngineer() {
        createUserIfConfigured(
                engineerEmail,
                engineerPassword,
                "Field Engineer",
                Role.ROLE_ENGINEER,
                Role.ROLE_CUSTOMER
        );
    }

    private void createUserIfConfigured(
            String email,
            String password,
            String name,
            Role... roles
    ) {
        if (!StringUtils.hasText(password)) {
            log.info("Skipping optional seed for {}: no password configured", email);
            return;
        }
        if (!StringUtils.hasText(email)) {
            log.warn("Skipping optional staff seed: email is blank");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("User already exists: {}", email);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setEnabled(true);

        for (Role role : roles) {
            user.addRole(role);
        }

        userRepository.save(user);

        log.info("Created user: {} with roles {}", email, java.util.Arrays.toString(roles));
    }
}
