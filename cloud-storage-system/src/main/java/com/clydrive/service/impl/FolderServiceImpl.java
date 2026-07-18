package com.clydrive.service.impl;

import com.clydrive.dtos.request.CreateFolderRequest;
import com.clydrive.dtos.response.BreadcrumbItem;
import com.clydrive.dtos.response.FileResponse;
import com.clydrive.dtos.response.FolderContentsResponse;
import com.clydrive.dtos.response.FolderResponse;
import com.clydrive.enums.AuditAction;
import com.clydrive.exception.ResourceAlreadyExistsException;
import com.clydrive.exception.ResourceNotFoundException;
import com.clydrive.module.AuditLog;
import com.clydrive.module.FileEntity;
import com.clydrive.module.Folder;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.FileRepository;
import com.clydrive.repository.FolderRepository;
import com.clydrive.security.SecurityUtils;
import com.clydrive.service.FolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class FolderServiceImpl implements FolderService {

    private static final int MAX_DEPTH = 64;

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final AuditLogRepository auditLogRepository;

    public FolderServiceImpl(
            FolderRepository folderRepository,
            FileRepository fileRepository,
            AuditLogRepository auditLogRepository) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String name = request.getName().trim();
        Long parentId = request.getParentId();

        log.info("[FOLDER_CREATE] Started | userId={} | parentId={} | name={}", userId, parentId, name);

        if (parentId != null) {
            requireOwnedFolder(parentId, userId);
        }

        boolean duplicate = parentId != null
                ? folderRepository.existsByUserIdAndParentIdAndName(userId, parentId, name)
                : folderRepository.existsByUserIdAndParentIdIsNullAndName(userId, name);
        if (duplicate) {
            throw new ResourceAlreadyExistsException("A folder named '" + name + "' already exists here");
        }

        Folder folder = folderRepository.save(Folder.builder()
                .userId(userId)
                .parentId(parentId)
                .name(name)
                .build());

        audit(AuditAction.FOLDER_CREATE, "Created folder '%s' (id=%d)".formatted(name, folder.getId()));
        return toResponse(folder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderResponse> listRootFolders() {
        Long userId = SecurityUtils.getCurrentUserId();
        return folderRepository.findByUserIdAndParentIdIsNull(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FolderContentsResponse getFolderContents(Long folderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Folder folder = requireOwnedFolder(folderId, userId);

        List<FolderResponse> subFolders = folderRepository.findByUserIdAndParentId(userId, folderId).stream()
                .map(this::toResponse)
                .toList();

        List<FileResponse> files = fileRepository.findByUserIdAndFolderId(userId, folderId).stream()
                .map(this::toFileResponse)
                .toList();

        return FolderContentsResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .breadcrumbs(buildBreadcrumbs(folder, userId))
                .subFolders(subFolders)
                .files(files)
                .build();
    }

    @Override
    @Transactional
    public FolderResponse renameFolder(Long folderId, String name) {
        Long userId = SecurityUtils.getCurrentUserId();
        String trimmed = name.trim();
        Folder folder = requireOwnedFolder(folderId, userId);

        boolean duplicate = folder.getParentId() != null
                ? folderRepository.existsByUserIdAndParentIdAndName(userId, folder.getParentId(), trimmed)
                : folderRepository.existsByUserIdAndParentIdIsNullAndName(userId, trimmed);
        if (duplicate && !trimmed.equals(folder.getName())) {
            throw new ResourceAlreadyExistsException("A folder named '" + trimmed + "' already exists here");
        }

        folder.setName(trimmed);
        folderRepository.save(folder);

        audit(AuditAction.FOLDER_RENAME, "Renamed folder id=%d to '%s'".formatted(folderId, trimmed));
        return toResponse(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long folderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Folder folder = requireOwnedFolder(folderId, userId);

        if (folderRepository.countByParentId(folderId) > 0 || fileRepository.countByFolderId(folderId) > 0) {
            throw new IllegalArgumentException("Folder is not empty");
        }

        folderRepository.delete(folder);
        audit(AuditAction.FOLDER_DELETE, "Deleted folder id=%d".formatted(folderId));
    }

    private List<BreadcrumbItem> buildBreadcrumbs(Folder folder, Long userId) {
        List<BreadcrumbItem> trail = new ArrayList<>();
        Folder current = folder;
        int depth = 0;
        while (current != null && depth < MAX_DEPTH) {
            trail.add(new BreadcrumbItem(current.getId(), current.getName()));
            Long parentId = current.getParentId();
            current = parentId != null
                    ? folderRepository.findByIdAndUserId(parentId, userId).orElse(null)
                    : null;
            depth++;
        }
        Collections.reverse(trail);
        return trail;
    }

    private Folder requireOwnedFolder(Long folderId, Long userId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + folderId));
    }

    private void audit(AuditAction action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    private FolderResponse toResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParentId())
                .createdAt(folder.getCreatedAt())
                .build();
    }

    private FileResponse toFileResponse(FileEntity entity) {
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
