package com.clydrive.initializer;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import com.clydrive.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${user.default-storage-quota}")
    private long defaultStorageQuota;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("[ADMIN_INIT] Checking for existing admin account");

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("[ADMIN_INIT] Admin already exists — skipping seed");
            return;
        }

        if (userRepository.existsByEmail(adminEmail) || userRepository.existsByUsername(adminUsername)) {
            log.warn("[ADMIN_INIT] Username or email already taken — skipping seed");
            return;
        }

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .phoneNumber("9999999999")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .storageQuota(defaultStorageQuota)
                .storageUsed(0L)
                .emailVerified(true)
                .phoneVerified(false)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build();

        userRepository.save(admin);

        log.info("[ADMIN_INIT] Admin created successfully | username={} | email={}",
                adminUsername, adminEmail);
    }
}
