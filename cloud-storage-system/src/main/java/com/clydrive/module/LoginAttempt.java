package com.clydrive.module;

import com.clydrive.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "login_attempts",
        indexes = @Index(name = "idx_login_attempts_user_id", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "login_identifier", length = 100)
    private String loginIdentifier;

    @Column(name = "identifier_type", length = 20)
    private String identifierType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status", length = 30)
    private AttemptStatus attemptStatus;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Builder.Default
    @Column(name = "account_locked")
    private boolean accountLocked = false;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @PrePersist
    public void onCreate() {
        if (attemptTime == null) {
            attemptTime = LocalDateTime.now();
        }
    }
}
