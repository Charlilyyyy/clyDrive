package com.clydrive.service.impl;

import com.clydrive.dtos.response.AdminStatsResponse;
import com.clydrive.dtos.response.AdminUserResponse;
import com.clydrive.enums.AuditAction;
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

    protected User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.clydrive.exception.ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    protected String currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
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
