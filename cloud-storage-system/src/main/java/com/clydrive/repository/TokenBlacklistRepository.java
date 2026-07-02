package com.clydrive.repository;

import com.clydrive.module.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByJti(String jti);

    void deleteByExpiryDateBefore(LocalDateTime date);
}
