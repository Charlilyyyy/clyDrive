package com.clydrive.repository;

import com.clydrive.module.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Page<FileEntity> findByUserId(Long userId, Pageable pageable);

    Page<FileEntity> findByUserIdAndFolderId(Long userId, Long folderId, Pageable pageable);

    List<FileEntity> findByUserIdAndFolderId(Long userId, Long folderId);

    Page<FileEntity> findByUserIdAndFolderIdIsNull(Long userId, Pageable pageable);

    Optional<FileEntity> findByIdAndUserId(Long id, Long userId);

    long countByFolderId(Long folderId);

    long countByUserId(Long userId);
}
