package com.clydrive.module;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "files",
        indexes = {
                @Index(name = "idx_files_user_id", columnList = "user_id"),
                @Index(name = "idx_files_folder_id", columnList = "folder_id"),
                @Index(name = "idx_files_user_folder", columnList = "user_id, folder_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
