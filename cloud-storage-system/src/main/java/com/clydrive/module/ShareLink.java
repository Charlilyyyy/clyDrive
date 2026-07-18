package com.clydrive.module;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "share_links",
        indexes = {
                @Index(name = "idx_share_file_id", columnList = "file_id"),
                @Index(name = "idx_share_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_share_token", columnNames = "token")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLink extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Builder.Default
    @Column(name = "download_count", nullable = false)
    private long downloadCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;
}
