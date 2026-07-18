package com.clydrive.service.impl;

import com.clydrive.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = "http://localhost:%d/api/v1/auth/verify-email?token=%s"
                .formatted(serverPort, token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("clyDrive — Verify Your Email");
        message.setText("""
                Hello,

                Welcome to clyDrive.

                Thank you for creating your account.

                To activate your account, please verify your email by clicking the link below:

                %s

                This verification link is valid for 15 minutes.

                If you did not create this account, please ignore this email.

                Regards,
                clyDrive Team
                """.formatted(verificationUrl));

        mailSender.send(message);
        log.info("[EMAIL] Verification email sent | to={}", to);
    }

    @Async
    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("clyDrive — Password Reset OTP");
        message.setText("""
                Hello,

                Your password reset OTP is: %s

                This OTP is valid for %d minutes.

                If you did not request this password reset, please ignore this email.

                Regards,
                clyDrive Team
                """.formatted(otp, otpExpiryMinutes));

        mailSender.send(message);
        log.info("[EMAIL] Password reset OTP email sent | to={}", to);
    }

    @Async
    @Override
    public void sendPasswordChangedEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("clyDrive — Your Password Was Changed");
        message.setText("""
                Hello,

                This is a confirmation that the password for your clyDrive account was changed.

                If you made this change, no further action is required.

                If you did NOT change your password, please reset it immediately and contact support.

                Regards,
                clyDrive Team
                """);

        mailSender.send(message);
        log.info("[EMAIL] Password changed notification sent | to={}", to);
    }
}
