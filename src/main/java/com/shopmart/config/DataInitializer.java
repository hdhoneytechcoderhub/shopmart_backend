//package com.shopmart.config;
//
//import com.shopmart.module.user.entity.Role;
//import com.shopmart.module.user.entity.User;
//import com.shopmart.module.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
///**
// * Seeds a default admin account on first run if it does not already exist.
// * Override the credentials with ADMIN_EMAIL / ADMIN_PASSWORD env vars.
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Value("${app.admin.email:admin@shopmart.local}")
//    private String adminEmail;
//
//    @Value("${app.admin.password:Admin@12345}")
//    private String adminPassword;
//
//    @Value("${app.superadmin.email:superadmin@shopmart.local}")
//    private String superAdminEmail;
//
//    @Value("${app.superadmin.password:Super@12345}")
//    private String superAdminPassword;
//
//    @Value("${app.engineer.email:engineer@shopmart.local}")
//    private String engineerEmail;
//
//    @Value("${app.engineer.password:Engineer@12345}")
//    private String engineerPassword;
//
//    @Override
//    public void run(String... args) {
//        seedAdmin();
//        seedSuperAdmin();
//        seedEngineer();
//    }
//
//    private void seedAdmin() {
//        if (userRepository.existsByEmail(adminEmail)) {
//            return;
//        }
//        User admin = new User();
//        admin.setName("ShopMart Admin");
//        admin.setEmail(adminEmail);
//        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
//        admin.setEmailVerified(true);
//        admin.setEnabled(true);
//        admin.addRole(Role.ROLE_ADMIN);
//        admin.addRole(Role.ROLE_CUSTOMER);
//        userRepository.save(admin);
//        log.info("Seeded default admin account: {}", adminEmail);
//    }
//
//    private void seedSuperAdmin() {
//        if (userRepository.existsByEmail(superAdminEmail)) {
//            return;
//        }
//        User su = new User();
//        su.setName("Super Admin");
//        su.setEmail(superAdminEmail);
//        su.setPasswordHash(passwordEncoder.encode(superAdminPassword));
//        su.setEmailVerified(true);
//        su.setEnabled(true);
//        su.addRole(Role.ROLE_SUPER_ADMIN);
//        su.addRole(Role.ROLE_ADMIN);
//        su.addRole(Role.ROLE_CUSTOMER);
//        userRepository.save(su);
//        log.info("Seeded default super admin account: {}", superAdminEmail);
//    }
//
//    private void seedEngineer() {
//        if (userRepository.existsByEmail(engineerEmail)) {
//            return;
//        }
//        User eng = new User();
//        eng.setName("Field Engineer");
//        eng.setEmail(engineerEmail);
//        eng.setPasswordHash(passwordEncoder.encode(engineerPassword));
//        eng.setEmailVerified(true);
//        eng.setEnabled(true);
//        eng.addRole(Role.ROLE_ENGINEER);
//        eng.addRole(Role.ROLE_CUSTOMER);
//        userRepository.save(eng);
//        log.info("Seeded default engineer account: {}", engineerEmail);
//    }
//}
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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@shopmart.local}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.superadmin.email:superadmin@shopmart.local}")
    private String superAdminEmail;

    @Value("${app.superadmin.password:Super@12345}")
    private String superAdminPassword;

    @Value("${app.engineer.email:engineer@shopmart.local}")
    private String engineerEmail;

    @Value("${app.engineer.password:Engineer@12345}")
    private String engineerPassword;

    @Override
    public void run(String... args) {
        log.info("Starting default user initialization...");

        seedAdmin();
        seedSuperAdmin();
        seedEngineer();

        log.info("Default user initialization completed.");
    }

    private void seedAdmin() {
        createUserIfNotExists(
                adminEmail,
                adminPassword,
                "ShopMart Admin",
                Role.ROLE_ADMIN,
                Role.ROLE_CUSTOMER
        );
    }

    private void seedSuperAdmin() {
        createUserIfNotExists(
                superAdminEmail,
                superAdminPassword,
                "Super Admin",
                Role.ROLE_SUPER_ADMIN,
                Role.ROLE_ADMIN,
                Role.ROLE_CUSTOMER
        );
    }

    private void seedEngineer() {
        createUserIfNotExists(
                engineerEmail,
                engineerPassword,
                "Field Engineer",
                Role.ROLE_ENGINEER,
                Role.ROLE_CUSTOMER
        );
    }

    private void createUserIfNotExists(
            String email,
            String password,
            String name,
            Role... roles
    ) {
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