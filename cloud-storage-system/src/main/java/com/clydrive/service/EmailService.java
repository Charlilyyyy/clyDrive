package com.clydrive.service;

public interface EmailService {

    void sendVerificationEmail(String to, String token);

    void sendOtpEmail(String to, String otp);

    void sendPasswordChangedEmail(String to);
}
