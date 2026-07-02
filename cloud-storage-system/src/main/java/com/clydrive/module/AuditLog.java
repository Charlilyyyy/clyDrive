package com.clydrive.module;

import com.clydrive.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_identifier", columnList = "identifier")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", length = 100)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "device_info", length = 1000)
    private String deviceInfo;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "details", length = 2000)
    private String details;

    @PrePersist
    public void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
