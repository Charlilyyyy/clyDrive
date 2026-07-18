package com.clydrive.scheduler;

import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.OtpRepository;
import com.clydrive.repository.RefreshTokenRepository;
import com.clydrive.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OtpRepository otpRepository;

    @Scheduled(cron = "${cleanup.cron:0 0 * * * *}")
    @Transactional
    public void purgeExpiredCredentials() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[CLEANUP] Purge started | at={}", now);

        refreshTokenRepository.deleteByExpiryDateBefore(now);
        refreshTokenRepository.deleteByRevokedTrue();
        tokenBlacklistRepository.deleteByExpiryDateBefore(now);
        emailVerificationTokenRepository.deleteByExpiryDateBefore(now);
        otpRepository.deleteByExpiryTimeBefore(now);

        log.info("[CLEANUP] Purge completed | at={}", LocalDateTime.now());
    }
}
