package com.clydrive.service;

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

    OtpResponse forgotPassword(ForgotPasswordRequest request);

    OtpResponse resendPasswordOtp(String email);

    EmailOtpVerifyResponse verifyPasswordOtp(VerifyPasswordOtpRequest request);

    ResetPasswordResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpServletRequest);

    ChangePasswordResponse changePassword(ChangePasswordRequest request, HttpServletRequest httpServletRequest);
}
