package com.clydrive.service.impl;

import com.clydrive.dtos.request.UserRegistrationRequest;
import com.clydrive.dtos.response.StorageResponse;
import com.clydrive.dtos.response.UserResponse;
import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.exception.ResourceAlreadyExistsException;
import com.clydrive.exception.ResourceNotFoundException;
import com.clydrive.module.EmailVerificationToken;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.security.SecurityUtils;
import com.clydrive.service.EmailService;
import com.clydrive.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${user.default-storage-quota}")
    private long defaultStorageQuota;

    public UserServiceImpl(
            UserRepository userRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("[REGISTER_USER] Registration started | username={}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "Username '" + request.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email '" + request.getEmail() + "' already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Phone number '" + request.getPhoneNumber() + "' already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .storageQuota(defaultStorageQuota)
                .storageUsed(0L)
                .emailVerified(false)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[REGISTER_USER] User saved | userId={}", savedUser.getId());

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .resendCount(0)
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        log.info("[REGISTER_USER] Registration completed | userId={} | username={}",
                savedUser.getId(), savedUser.getUsername());

        return mapToResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageResponse getMyStorage() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long used = user.getStorageUsed();
        long quota = user.getStorageQuota();
        long available = Math.max(0, quota - used);
        double percentage = quota > 0
                ? Math.round((used * 10000.0) / quota) / 100.0
                : 0.0;

        return StorageResponse.builder()
                .storageUsed(used)
                .storageQuota(quota)
                .storageAvailable(available)
                .usagePercentage(percentage)
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
