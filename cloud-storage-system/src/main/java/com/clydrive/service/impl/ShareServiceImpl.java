package com.clydrive.service.impl;

import com.clydrive.dtos.request.CreateShareRequest;
import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.ShareResponse;
import com.clydrive.enums.AuditAction;
import com.clydrive.exception.ResourceNotFoundException;
import com.clydrive.module.AuditLog;
import com.clydrive.module.FileEntity;
import com.clydrive.module.ShareLink;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.FileRepository;
import com.clydrive.repository.ShareLinkRepository;
import com.clydrive.security.SecurityUtils;
import com.clydrive.service.FileStorageService;
import com.clydrive.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ShareServiceImpl implements ShareService {

    private static final int DEFAULT_EXPIRY_HOURS = 24;

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public ShareServiceImpl(
            ShareLinkRepository shareLinkRepository,
            FileRepository fileRepository,
            AuditLogRepository auditLogRepository,
            FileStorageService fileStorageService,
            PasswordEncoder passwordEncoder) {
        this.shareLinkRepository = shareLinkRepository;
        this.fileRepository = fileRepository;
        this.auditLogRepository = auditLogRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public ShareResponse createShare(Long fileId, CreateShareRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        int expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : DEFAULT_EXPIRY_HOURS;
        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        String passwordHash = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null;

        ShareLink share = shareLinkRepository.save(ShareLink.builder()
                .fileId(fileId)
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .passwordHash(passwordHash)
                .downloadCount(0L)
                .revoked(false)
                .build());

        audit(AuditAction.SHARE_CREATE, "Created share for file id=%d (expires %s)".formatted(fileId, share.getExpiresAt()));
        log.info("[SHARE_CREATE] Completed | fileId={} | shareId={}", fileId, share.getId());
        return toResponse(share);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareResponse> listShares(Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();

        fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        return shareLinkRepository.findByFileIdAndRevokedFalse(fileId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void revokeShare(String token) {
        Long userId = SecurityUtils.getCurrentUserId();

        ShareLink share = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!share.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Share link not found");
        }

        share.setRevoked(true);
        shareLinkRepository.save(share);

        audit(AuditAction.SHARE_REVOKE, "Revoked share token for file id=%d".formatted(share.getFileId()));
        log.info("[SHARE_REVOKE] Completed | fileId={}", share.getFileId());
    }

    @Override
    @Transactional
    public DownloadableFile accessSharedFile(String token, String password) {
        ShareLink share = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (share.isRevoked()) {
            throw new IllegalArgumentException("Share link has been revoked");
        }

        if (share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Share link has expired");
        }

        if (share.getPasswordHash() != null) {
            if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, share.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid or missing share password");
            }
        }

        FileEntity file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("Shared file no longer exists"));

        Resource resource = fileStorageService.load(file.getStoragePath());

        share.setDownloadCount(share.getDownloadCount() + 1);
        shareLinkRepository.save(share);

        audit(AuditAction.SHARE_DOWNLOAD, "Shared file id=%d accessed via token".formatted(file.getId()));
        log.info("[SHARE_DOWNLOAD] Completed | fileId={}", file.getId());

        return DownloadableFile.builder()
                .resource(resource)
                .fileName(file.getOriginalName())
                .contentType(file.getFileType() != null ? file.getFileType() : "application/octet-stream")
                .size(file.getSize())
                .build();
    }

    private void audit(AuditAction action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    private ShareResponse toResponse(ShareLink share) {
        return ShareResponse.builder()
                .id(share.getId())
                .fileId(share.getFileId())
                .token(share.getToken())
                .passwordProtected(share.getPasswordHash() != null)
                .downloadCount(share.getDownloadCount())
                .expiresAt(share.getExpiresAt())
                .revoked(share.isRevoked())
                .createdAt(share.getCreatedAt())
                .build();
    }
}
