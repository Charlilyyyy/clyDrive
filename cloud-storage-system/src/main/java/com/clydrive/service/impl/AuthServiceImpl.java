package com.clydrive.service.impl;

import com.clydrive.config.SecurityProperties;
import com.clydrive.dtos.request.ChangePasswordRequest;
import com.clydrive.dtos.request.ForgotPasswordRequest;
import com.clydrive.dtos.request.LoginRequest;
import com.clydrive.dtos.request.RefreshTokenRequest;
import com.clydrive.dtos.request.ResetPasswordRequest;
import com.clydrive.dtos.request.VerifyPasswordOtpRequest;
import com.clydrive.dtos.response.ChangePasswordResponse;
import com.clydrive.dtos.response.EmailOtpVerifyResponse;
import com.clydrive.dtos.response.EmailVerificationResponse;
import com.clydrive.dtos.response.LoginHistoryResponse;
import com.clydrive.dtos.response.LoginResponse;
import com.clydrive.dtos.response.LogoutResponse;
import com.clydrive.dtos.response.OtpResponse;
import com.clydrive.dtos.response.ResendVerificationEmailResponse;
import com.clydrive.dtos.response.ResetPasswordResponse;
import com.clydrive.dtos.response.TokenData;
import com.clydrive.dtos.response.TokenResponse;
import com.clydrive.enums.AttemptStatus;
import com.clydrive.enums.AuditAction;
import com.clydrive.enums.OtpPurpose;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.AuditLog;
import com.clydrive.module.EmailVerificationToken;
import com.clydrive.module.LoginAttempt;
import com.clydrive.module.Otp;
import com.clydrive.module.PasswordHistory;
import com.clydrive.module.RefreshToken;
import com.clydrive.module.User;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.LoginAttemptRepository;
import com.clydrive.repository.OtpRepository;
import com.clydrive.repository.PasswordHistoryRepository;
import com.clydrive.repository.RefreshTokenRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.service.AuthService;
import com.clydrive.service.EmailService;
import com.clydrive.service.TokenBlacklistService;
import com.clydrive.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_EXPIRY_MINUTES = 15;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS_PER_HOUR = 3;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OtpRepository otpRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginAttemptRepository loginAttemptRepository,
            AuditLogRepository auditLogRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            OtpRepository otpRepository,
            PasswordHistoryRepository passwordHistoryRepository,
            TokenBlacklistService tokenBlacklistService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.otpRepository = otpRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.securityProperties = securityProperties;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        String identifier = request.getIdentifier().trim();
        log.info("[LOGIN] Login started | identifier={}", identifier);

        User user = userRepository
                .findByUsernameOrEmailOrPhoneNumber(identifier, identifier, identifier)
                .orElseThrow(() -> {
                    saveUnknownUserAttempt(identifier, httpServletRequest);
                    saveAuditLog(identifier, AuditAction.LOGIN_FAILED, httpServletRequest, "User not found");
                    return new RuntimeException("Invalid username/email/phone or password");
                });

        validateAccountStatus(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            processFailedLogin(user, identifier, httpServletRequest);
            throw new RuntimeException("Invalid username/email/phone or password");
        }

        resetFailedAttempts(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        saveSuccessfulAttempt(user, identifier, httpServletRequest);
        saveAuditLog(user.getEmail(), AuditAction.LOGIN, httpServletRequest, "User logged in successfully");

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiryDate(jwtUtil.extractExpiration(refreshToken))
                .revoked(false)
                .build());

        log.info("[LOGIN] Login completed | userId={} | username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .username(user.getUsername())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .tokens(TokenData.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public LogoutResponse logout(String accessToken, String refreshToken) {
        log.info("[LOGOUT] Logout started");

        tokenBlacklistService.blacklistToken(accessToken);

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        log.info("[LOGOUT] Logout completed | userId={}", token.getUserId());

        return LogoutResponse.builder()
                .loggedOut(true)
                .tokenRevoked(true)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        log.info("[REFRESH_TOKEN] Refresh started");

        String refreshTokenValue = request.getRefreshToken();

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (!jwtUtil.isTokenValid(refreshTokenValue)
                || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        Long userId = jwtUtil.extractUserId(refreshTokenValue);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(newRefreshToken)
                .expiryDate(jwtUtil.extractExpiration(newRefreshToken))
                .revoked(false)
                .build());

        log.info("[REFRESH_TOKEN] Refresh completed | userId={}", user.getId());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getLoginHistory(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) Objects.requireNonNull(authentication).getPrincipal();
        Long currentUserId = Long.parseLong(userDetails.getUsername());

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        if (!isAdmin && !currentUserId.equals(userId)) {
            throw new RuntimeException("You can only view your own login history");
        }

        return loginAttemptRepository.findByUserIdOrderByAttemptTimeDesc(userId).stream()
                .map(attempt -> LoginHistoryResponse.builder()
                        .status(attempt.getAttemptStatus() != null ? attempt.getAttemptStatus().name() : null)
                        .ip(attempt.getIpAddress())
                        .device(attempt.getDeviceInfo())
                        .time(attempt.getAttemptTime())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public EmailVerificationResponse verifyEmail(String token) {
        log.info("[VERIFY_EMAIL] Verification started");

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new RuntimeException("Verification token already used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        auditLogRepository.save(AuditLog.builder()
                .identifier(user.getEmail())
                .action(AuditAction.EMAIL_VERIFIED)
                .timestamp(LocalDateTime.now())
                .details("Email verified and account activated")
                .build());

        log.info("[VERIFY_EMAIL] Completed | userId={} | username={}", user.getId(), user.getUsername());

        return EmailVerificationResponse.builder()
                .verified(true)
                .message("Account activated")
                .build();
    }

    @Override
    @Transactional
    public ResendVerificationEmailResponse resendVerificationEmail(String email) {
        log.info("[RESEND_VERIFICATION_EMAIL] Started | email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        long resendCount = emailVerificationTokenRepository.countByUserAndCreatedAtAfter(
                user, LocalDateTime.now().minusHours(1));
        if (resendCount >= MAX_RESENDS_PER_HOUR) {
            throw new RuntimeException("Maximum resend attempts reached. Try again after 1 hour.");
        }

        EmailVerificationToken latestToken = emailVerificationTokenRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);
        if (latestToken != null
                && latestToken.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting another verification email.");
        }

        emailVerificationTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.flush();

        String token = UUID.randomUUID().toString();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .used(false)
                .resendCount((int) resendCount + 1)
                .expiryDate(LocalDateTime.now().plusMinutes(VERIFICATION_EXPIRY_MINUTES))
                .build());

        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("[RESEND_VERIFICATION_EMAIL] Completed | userId={}", user.getId());

        return ResendVerificationEmailResponse.builder()
                .emailSent(true)
                .email(user.getEmail())
                .expiryMinutes(VERIFICATION_EXPIRY_MINUTES)
                .build();
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("[FORGOT_PASSWORD] Started | email={}", request.getEmail());
        return issuePasswordOtp(request.getEmail());
    }

    @Override
    @Transactional
    public OtpResponse resendPasswordOtp(String email) {
        log.info("[RESEND_PASSWORD_OTP] Started | email={}", email);
        return issuePasswordOtp(email);
    }

    @Override
    @Transactional
    public EmailOtpVerifyResponse verifyPasswordOtp(VerifyPasswordOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otpCode = request.getOtp().trim();

        log.info("[VERIFY_PASSWORD_OTP] Started | email={}", email);

        Otp otp = otpRepository
                .findByEmailAndOtpCodeAndPurpose(email, otpCode, OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (otp.isVerified()) {
            throw new RuntimeException("OTP already verified");
        }

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("[VERIFY_PASSWORD_OTP] Completed | email={}", email);

        return EmailOtpVerifyResponse.builder()
                .verified(true)
                .email(email)
                .canResetPassword(true)
                .build();
    }

    private OtpResponse issuePasswordOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        otpRepository.deleteByUserAndPurpose(user, OtpPurpose.FORGOT_PASSWORD);
        otpRepository.flush();

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        otpRepository.save(Otp.builder()
                .user(user)
                .email(user.getEmail())
                .otpCode(otpCode)
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .verified(false)
                .attemptCount(0)
                .expiryTime(expiryTime)
                .build());

        emailService.sendOtpEmail(user.getEmail(), otpCode);

        log.info("[FORGOT_PASSWORD] OTP issued | userId={} | expiresAt={}", user.getId(), expiryTime);

        return OtpResponse.builder()
                .email(user.getEmail())
                .expiryMinutes(otpExpiryMinutes)
                .emailSent(true)
                .build();
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpServletRequest) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("[RESET_PASSWORD] Started | email={}", email);

        Otp otp = otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (!otp.isVerified()) {
            throw new RuntimeException("OTP verification required");
        }

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be same as current password");
        }

        validatePasswordHistory(user.getId(), request.getNewPassword());

        savePasswordHistory(user.getId(), user.getPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);

        otp.setVerified(false);
        otpRepository.save(otp);

        saveAuditLog(user.getEmail(), AuditAction.PASSWORD_CHANGE, httpServletRequest, "Password reset via OTP");
        emailService.sendPasswordChangedEmail(user.getEmail());

        log.info("[RESET_PASSWORD] Completed | userId={} | revokedTokens={}", user.getId(), activeTokens.size());

        return ResetPasswordResponse.builder()
                .passwordUpdated(true)
                .tokensRevoked(true)
                .build();
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request, HttpServletRequest httpServletRequest) {
        log.info("[CHANGE_PASSWORD] Started");

        String token = resolveToken(httpServletRequest);
        Long userId = jwtUtil.extractUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be same as current password");
        }

        validatePasswordHistory(user.getId(), request.getNewPassword());

        savePasswordHistory(user.getId(), user.getPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        saveAuditLog(user.getEmail(), AuditAction.PASSWORD_CHANGE, httpServletRequest, "Password changed by user");
        emailService.sendPasswordChangedEmail(user.getEmail());

        log.info("[CHANGE_PASSWORD] Completed | userId={}", user.getId());

        return ChangePasswordResponse.builder()
                .passwordChanged(true)
                .message("Password changed successfully")
                .build();
    }

    private void validatePasswordHistory(Long userId, String newPassword) {
        int limit = securityProperties.getPasswordHistoryLimit();
        List<PasswordHistory> history = passwordHistoryRepository
                .findTop5ByUserIdOrderByChangedAtDesc(userId);

        for (PasswordHistory entry : history.stream().limit(limit).toList()) {
            if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw new RuntimeException("You cannot reuse your last " + limit + " passwords");
            }
        }
    }

    private void savePasswordHistory(Long userId, String passwordHash) {
        passwordHistoryRepository.save(PasswordHistory.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return header.substring(7);
    }

    private void validateAccountStatus(User user) {
        if (!user.isEnabled() || user.getStatus() == UserStatus.DISABLED) {
            throw new RuntimeException("Account is disabled");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Account is deleted");
        }

        if (!user.isEmailVerified() || user.getStatus() == UserStatus.PENDING) {
            throw new RuntimeException("Please verify your email first");
        }

        if (!user.isAccountNonLocked() || user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Account locked until " + user.getLockedUntil());
            }

            user.setFailedAttempts(0);
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            auditLogRepository.save(AuditLog.builder()
                    .identifier(user.getEmail())
                    .action(AuditAction.ACCOUNT_UNLOCKED)
                    .timestamp(LocalDateTime.now())
                    .details("Account automatically unlocked after lock period")
                    .build());
        }
    }

    private void processFailedLogin(User user, String identifier, HttpServletRequest request) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        boolean locked = false;
        if (attempts >= securityProperties.getMaxFailedAttempts()) {
            user.setAccountNonLocked(false);
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(securityProperties.getLockTimeMinutes()));
            locked = true;
        }

        userRepository.save(user);

        loginAttemptRepository.save(LoginAttempt.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .loginIdentifier(identifier)
                .identifierType(getIdentifierType(identifier))
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .deviceInfo(getDevice(request))
                .attemptStatus(locked ? AttemptStatus.ACCOUNT_LOCKED : AttemptStatus.FAILED)
                .failureReason(locked ? "ACCOUNT_LOCKED" : "INVALID_PASSWORD")
                .failedAttempts(attempts)
                .accountLocked(locked)
                .lockTime(locked ? user.getLockedUntil() : null)
                .attemptTime(LocalDateTime.now())
                .build());

        saveAuditLog(
                identifier,
                locked ? AuditAction.ACCOUNT_LOCKED : AuditAction.LOGIN_FAILED,
                request,
                locked ? "Account locked after maximum failed attempts" : "Invalid password");
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
    }

    private void saveSuccessfulAttempt(User user, String identifier, HttpServletRequest request) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .loginIdentifier(identifier)
                .identifierType(getIdentifierType(identifier))
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .deviceInfo(getDevice(request))
                .attemptStatus(AttemptStatus.SUCCESS)
                .failedAttempts(0)
                .accountLocked(false)
                .attemptTime(LocalDateTime.now())
                .build());
    }

    private void saveUnknownUserAttempt(String identifier, HttpServletRequest request) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .loginIdentifier(identifier)
                .identifierType(getIdentifierType(identifier))
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .deviceInfo(getDevice(request))
                .attemptStatus(AttemptStatus.FAILED)
                .failureReason("USER_NOT_FOUND")
                .accountLocked(false)
                .attemptTime(LocalDateTime.now())
                .build());
    }

    private void saveAuditLog(String identifier, AuditAction action, HttpServletRequest request, String details) {
        auditLogRepository.save(AuditLog.builder()
                .identifier(identifier)
                .action(action)
                .ipAddress(getClientIp(request))
                .deviceInfo(getDevice(request))
                .timestamp(LocalDateTime.now())
                .details(details)
                .build());
    }

    private String getIdentifierType(String identifier) {
        if (identifier.contains("@")) {
            return "EMAIL";
        }
        if (identifier.matches("\\d+")) {
            return "PHONE";
        }
        return "USERNAME";
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank() && !"unknown".equalsIgnoreCase(xfHeader)) {
            return xfHeader.split(",")[0].trim();
        }

        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String getDevice(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return "UNKNOWN";
        }

        String browser = "Browser";
        String os = "OS";
        String deviceType = "Desktop";

        if (ua.contains("Edg")) {
            browser = "Edge";
        } else if (ua.contains("Chrome")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox")) {
            browser = "Firefox";
        } else if (ua.contains("Safari")) {
            browser = "Safari";
        }

        if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("iPhone") || ua.contains("iOS")) {
            os = "iOS";
        } else if (ua.contains("Mac")) {
            os = "Mac";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        }

        if (ua.contains("Mobile")) {
            deviceType = "Mobile";
        } else if (ua.contains("Tablet")) {
            deviceType = "Tablet";
        }

        return browser + " | " + os + " | " + deviceType;
    }
}
