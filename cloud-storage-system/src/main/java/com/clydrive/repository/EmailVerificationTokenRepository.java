package com.clydrive.repository;

import com.clydrive.module.EmailVerificationToken;
import com.clydrive.module.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);

    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(LocalDateTime cutoff);
}
