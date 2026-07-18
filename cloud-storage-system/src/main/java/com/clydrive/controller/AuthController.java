package com.clydrive.controller;

import com.clydrive.dtos.request.LoginRequest;
import com.clydrive.dtos.request.RefreshTokenRequest;
import com.clydrive.dtos.request.ResendVerificationRequest;
import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.EmailVerificationResponse;
import com.clydrive.dtos.response.LoginHistoryResponse;
import com.clydrive.dtos.response.LoginResponse;
import com.clydrive.dtos.response.LogoutResponse;
import com.clydrive.dtos.response.ResendVerificationEmailResponse;
import com.clydrive.dtos.response.TokenResponse;
import com.clydrive.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("[LOGIN] Request received | identifier={} | ip={} | uri={}",
                request.getIdentifier(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getRequestURI());

        LoginResponse response = authService.login(request, httpServletRequest);

        log.info("[LOGIN] Completed | username={}", response.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Login successful",
                        httpServletRequest.getRequestURI(),
                        response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest servletRequest) {

        String accessToken = authorizationHeader.replace("Bearer ", "").trim();

        log.info("[LOGOUT] Request received | ip={} | uri={}",
                servletRequest.getRemoteAddr(),
                servletRequest.getRequestURI());

        LogoutResponse logoutResponse = authService.logout(accessToken, request.getRefreshToken());

        log.info("[LOGOUT] Completed");

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Logout successful",
                        servletRequest.getRequestURI(),
                        logoutResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("[REFRESH_TOKEN] Request received | ip={} | uri={}",
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getRequestURI());

        TokenResponse response = authService.refreshToken(request, httpServletRequest);

        log.info("[REFRESH_TOKEN] Completed");

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Token refreshed successfully",
                        httpServletRequest.getRequestURI(),
                        response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpServletRequest) {

        log.info("[VERIFY_EMAIL] Request received | ip={} | uri={}",
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getRequestURI());

        EmailVerificationResponse response = authService.verifyEmail(token);

        log.info("[VERIFY_EMAIL] Completed");

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Email verified successfully",
                        httpServletRequest.getRequestURI(),
                        response));
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<ResendVerificationEmailResponse>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpServletRequest) {

        log.info("[RESEND_VERIFICATION_EMAIL] Request received | email={} | ip={} | uri={}",
                request.getEmail(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getRequestURI());

        ResendVerificationEmailResponse response = authService.resendVerificationEmail(request.getEmail());

        log.info("[RESEND_VERIFICATION_EMAIL] Completed | email={}", request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Verification email resent successfully",
                        httpServletRequest.getRequestURI(),
                        response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse<List<LoginHistoryResponse>>> getLoginHistory(
            @RequestParam Long userId,
            HttpServletRequest httpServletRequest) {

        log.info("[LOGIN_HISTORY] Request received | userId={} | ip={} | uri={}",
                userId,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getRequestURI());

        List<LoginHistoryResponse> history = authService.getLoginHistory(userId);

        log.info("[LOGIN_HISTORY] Completed | userId={} | records={}", userId, history.size());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Login history fetched successfully",
                        httpServletRequest.getRequestURI(),
                        history));
    }
}
