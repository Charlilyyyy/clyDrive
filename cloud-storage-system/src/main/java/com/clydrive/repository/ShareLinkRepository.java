package com.clydrive.repository;

import com.clydrive.module.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByFileIdAndRevokedFalse(Long fileId);

    List<ShareLink> findByUserIdAndFileId(Long userId, Long fileId);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByRevokedTrueAndExpiresAtBefore(LocalDateTime now);
}
