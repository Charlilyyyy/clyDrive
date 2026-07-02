package com.clydrive.repository;

import com.clydrive.module.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserIdAndParentIdIsNull(Long userId);

    List<Folder> findByUserIdAndParentId(Long userId, Long parentId);

    Optional<Folder> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndParentIdAndName(Long userId, Long parentId, String name);

    boolean existsByUserIdAndParentIdIsNullAndName(Long userId, String name);

    long countByParentId(Long parentId);
}
