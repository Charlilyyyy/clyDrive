package com.clydrive.service.impl;

import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.FileResponse;
import com.clydrive.dtos.response.PageResponse;
import com.clydrive.enums.AuditAction;
import com.clydrive.exception.ResourceNotFoundException;
import com.clydrive.module.AuditLog;
import com.clydrive.module.FileEntity;
import com.clydrive.module.User;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.FileRepository;
import com.clydrive.repository.FolderRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.security.SecurityUtils;
import com.clydrive.service.FileService;
import com.clydrive.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;

    @Value("${storage.max-file-size}")
    private long maxFileSize;

    public FileServiceImpl(
            FileRepository fileRepository,
            FolderRepository folderRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public FileResponse upload(MultipartFile file, Long folderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[FILE_UPLOAD] Started | userId={} | folderId={}", userId, folderId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File exceeds the maximum allowed size");
        }

        if (folderId != null) {
            folderRepository.findByIdAndUserId(folderId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + folderId));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStorageUsed() + file.getSize() > user.getStorageQuota()) {
            throw new IllegalArgumentException("Storage quota exceeded");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = StringUtils.getFilenameExtension(originalName);
        String storedName = UUID.randomUUID() + (extension != null ? "." + extension : "");

        String storagePath = fileStorageService.store(file, userId, storedName);

        FileEntity entity = fileRepository.save(FileEntity.builder()
                .userId(userId)
                .folderId(folderId)
                .fileName(storedName)
                .originalName(originalName)
                .fileType(file.getContentType())
                .size(file.getSize())
                .storagePath(storagePath)
                .uploadedAt(LocalDateTime.now())
                .build());

        user.setStorageUsed(user.getStorageUsed() + file.getSize());
        userRepository.save(user);

        audit(user.getEmail(), AuditAction.FILE_UPLOAD, "Uploaded file '%s' (%d bytes)".formatted(originalName, file.getSize()));

        log.info("[FILE_UPLOAD] Completed | fileId={} | userId={}", entity.getId(), userId);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableFile download(Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        FileEntity entity = requireOwnedFile(fileId, userId);

        Resource resource = fileStorageService.load(entity.getStoragePath());

        auditLogRepository.save(AuditLog.builder()
                .action(AuditAction.FILE_DOWNLOAD)
                .timestamp(LocalDateTime.now())
                .details("Downloaded file id=" + fileId)
                .build());

        return DownloadableFile.builder()
                .resource(resource)
                .fileName(entity.getOriginalName())
                .contentType(entity.getFileType() != null ? entity.getFileType() : "application/octet-stream")
                .size(entity.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> listFiles(Long folderId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();

        Page<FileEntity> page = folderId != null
                ? fileRepository.findByUserIdAndFolderId(userId, folderId, pageable)
                : fileRepository.findByUserIdAndFolderIdIsNull(userId, pageable);

        return PageResponse.from(page, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFile(Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return toResponse(requireOwnedFile(fileId, userId));
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        FileEntity entity = requireOwnedFile(fileId, userId);

        fileStorageService.delete(entity.getStoragePath());
        fileRepository.delete(entity);

        userRepository.findById(userId).ifPresent(user -> {
            user.setStorageUsed(Math.max(0, user.getStorageUsed() - entity.getSize()));
            userRepository.save(user);
            audit(user.getEmail(), AuditAction.FILE_DELETE, "Deleted file '%s'".formatted(entity.getOriginalName()));
        });

        log.info("[FILE_DELETE] Completed | fileId={} | userId={}", fileId, userId);
    }

    private FileEntity requireOwnedFile(Long fileId, Long userId) {
        return fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
    }

    private void audit(String identifier, AuditAction action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .identifier(identifier)
                .action(action)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    private FileResponse toResponse(FileEntity entity) {
        return FileResponse.builder()
                .id(entity.getId())
                .name(entity.getOriginalName())
                .type(entity.getFileType())
                .size(entity.getSize())
                .folderId(entity.getFolderId())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
