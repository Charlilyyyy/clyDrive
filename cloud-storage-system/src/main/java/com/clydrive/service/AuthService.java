package com.clydrive.service;

import com.clydrive.dtos.request.LoginRequest;
import com.clydrive.dtos.request.RefreshTokenRequest;
import com.clydrive.dtos.response.EmailVerificationResponse;
import com.clydrive.dtos.response.LoginHistoryResponse;
import com.clydrive.dtos.response.LoginResponse;
import com.clydrive.dtos.response.LogoutResponse;
import com.clydrive.dtos.response.ResendVerificationEmailResponse;
import com.clydrive.dtos.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest);

    LogoutResponse logout(String accessToken, String refreshToken);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpServletRequest);

    List<LoginHistoryResponse> getLoginHistory(Long userId);

    EmailVerificationResponse verifyEmail(String token);

    ResendVerificationEmailResponse resendVerificationEmail(String email);
}
