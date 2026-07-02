package com.clydrive.repository;

import com.clydrive.enums.OtpPurpose;
import com.clydrive.module.Otp;
import com.clydrive.module.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    void deleteByUserAndPurpose(User user, OtpPurpose purpose);

    Optional<Otp> findByEmailAndOtpCodeAndPurpose(String email, String otpCode, OtpPurpose purpose);

    Optional<Otp> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    void deleteByExpiryTimeBefore(LocalDateTime now);
}
