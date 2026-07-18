package com.clydrive.service.impl;

import com.clydrive.dtos.response.AdminStatsResponse;
import com.clydrive.dtos.response.AdminUserResponse;
import com.clydrive.dtos.response.LockUserResponse;
import com.clydrive.dtos.response.UnlockUserResponse;
import com.clydrive.enums.AuditAction;
import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.AuditLog;
import com.clydrive.module.User;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminServiceImpl(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        log.info("[ADMIN_STATS] Retrieval started");

        List<User> users = userRepository.findAll();
        long totalStorageUsed = users.stream().mapToLong(User::getStorageUsed).sum();

        return AdminStatsResponse.builder()
                .totalUsers(users.size())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .pendingUsers(userRepository.countByStatus(UserStatus.PENDING))
                .lockedUsers(userRepository.countByStatus(UserStatus.LOCKED))
                .disabledUsers(userRepository.countByStatus(UserStatus.DISABLED))
                .deletedUsers(userRepository.countByStatus(UserStatus.DELETED))
                .totalStorageUsed(totalStorageUsed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        log.info("[ADMIN_GET_ALL_USERS] Retrieval started");
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        log.info("[ADMIN_GET_USER] Retrieval started | userId={}", userId);
        return mapToResponse(findUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(Long userId, Role role) {
        log.info("[ADMIN_UPDATE_ROLE] Started | userId={} | role={}", userId, role);

        User user = findUser(userId);
        guardDefaultAdmin(user, "Default admin role cannot be modified");

        if (user.getId().equals(currentAdminId()) && role == Role.USER) {
            throw new IllegalArgumentException("You cannot remove your own admin role");
        }

        if (user.getRole() == role) {
            throw new IllegalArgumentException("User already has role: " + role);
        }

        Role previous = user.getRole();
        user.setRole(role);
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_UPDATE_ROLE,
                "Admin %s changed role of '%s' from %s to %s".formatted(currentAdminId(), user.getEmail(), previous, role));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public LockUserResponse lockUser(Long userId) {
        log.info("[ADMIN_LOCK_USER] Started | userId={}", userId);

        User user = findUser(userId);
        guardDefaultAdmin(user, "Default admin account cannot be locked");
        guardSelf(user, "You cannot lock your own account");

        if (!user.isAccountNonLocked()) {
            throw new IllegalArgumentException("User account is already locked");
        }

        user.setAccountNonLocked(false);
        user.setStatus(UserStatus.LOCKED);
        user.setLockedUntil(null);
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_LOCK_USER,
                "Admin %s locked account '%s'".formatted(currentAdminId(), user.getEmail()));

        return LockUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonLocked(false)
                .lockedUntil(user.getLockedUntil())
                .lockedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public UnlockUserResponse unlockUser(Long userId) {
        log.info("[ADMIN_UNLOCK_USER] Started | userId={}", userId);

        User user = findUser(userId);

        if (user.isAccountNonLocked() && user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalArgumentException("User account is already unlocked");
        }

        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_UNLOCK_USER,
                "Admin %s unlocked account '%s'".formatted(currentAdminId(), user.getEmail()));

        return UnlockUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accountNonLocked(true)
                .failedAttempts(user.getFailedAttempts())
                .unlockedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponse enableUser(Long userId) {
        log.info("[ADMIN_ENABLE_USER] Started | userId={}", userId);

        User user = findUser(userId);
        guardDefaultAdmin(user, "Default admin account is always enabled");

        if (user.isEnabled() && user.getStatus() != UserStatus.DISABLED) {
            throw new IllegalArgumentException("User account is already enabled");
        }

        user.setEnabled(true);
        user.setStatus(user.isEmailVerified() ? UserStatus.ACTIVE : UserStatus.PENDING);
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_ENABLE_USER,
                "Admin %s enabled account '%s'".formatted(currentAdminId(), user.getEmail()));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse disableUser(Long userId) {
        log.info("[ADMIN_DISABLE_USER] Started | userId={}", userId);

        User user = findUser(userId);
        guardDefaultAdmin(user, "Default admin account cannot be disabled");
        guardSelf(user, "You cannot disable your own account");

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is already disabled");
        }

        user.setEnabled(false);
        user.setStatus(UserStatus.DISABLED);
        user.setAccountNonLocked(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_DISABLE_USER,
                "Admin %s disabled account '%s'".formatted(currentAdminId(), user.getEmail()));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse deleteUser(Long userId) {
        log.info("[ADMIN_DELETE_USER] Started | userId={}", userId);

        User user = findUser(userId);
        guardDefaultAdmin(user, "Default admin account cannot be deleted");
        guardSelf(user, "You cannot delete your own account");

        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("User is already deleted");
        }

        user.setStatus(UserStatus.DELETED);
        user.setEnabled(false);
        user.setAccountNonLocked(false);
        userRepository.save(user);

        audit(user, AuditAction.ADMIN_DELETE_USER,
                "Admin %s deleted account '%s'".formatted(currentAdminId(), user.getEmail()));

        return mapToResponse(user);
    }

    private void guardDefaultAdmin(User user, String message) {
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void guardSelf(User user, String message) {
        if (user.getId().equals(currentAdminId())) {
            throw new IllegalArgumentException(message);
        }
    }

    protected User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.clydrive.exception.ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    protected Long currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected void audit(User user, AuditAction action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .identifier(user.getEmail())
                .action(action)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    protected AdminUserResponse mapToResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .accountNonLocked(user.isAccountNonLocked())
                .storageUsed(user.getStorageUsed())
                .storageQuota(user.getStorageQuota())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
