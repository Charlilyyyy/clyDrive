package com.clydrive.service.impl;

import com.clydrive.config.SecurityProperties;
import com.clydrive.dtos.request.LoginRequest;
import com.clydrive.dtos.request.RefreshTokenRequest;
import com.clydrive.dtos.response.LoginHistoryResponse;
import com.clydrive.dtos.response.LoginResponse;
import com.clydrive.dtos.response.LogoutResponse;
import com.clydrive.dtos.response.TokenData;
import com.clydrive.dtos.response.TokenResponse;
import com.clydrive.enums.AttemptStatus;
import com.clydrive.enums.AuditAction;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.AuditLog;
import com.clydrive.module.LoginAttempt;
import com.clydrive.module.RefreshToken;
import com.clydrive.module.User;
import com.clydrive.repository.AuditLogRepository;
import com.clydrive.repository.LoginAttemptRepository;
import com.clydrive.repository.RefreshTokenRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.service.AuthService;
import com.clydrive.service.TokenBlacklistService;
import com.clydrive.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginAttemptRepository loginAttemptRepository,
            AuditLogRepository auditLogRepository,
            TokenBlacklistService tokenBlacklistService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.tokenBlacklistService = tokenBlacklistService;
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
