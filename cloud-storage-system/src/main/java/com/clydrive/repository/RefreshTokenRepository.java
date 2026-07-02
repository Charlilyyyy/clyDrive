package com.clydrive.repository;

import com.clydrive.module.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    boolean existsByToken(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    void deleteByExpiryDateBefore(LocalDateTime now);

    void deleteByRevokedTrue();
}
