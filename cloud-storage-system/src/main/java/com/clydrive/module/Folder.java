package com.clydrive.module;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "folders",
        indexes = {
                @Index(name = "idx_folders_user_id", columnList = "user_id"),
                @Index(name = "idx_folders_parent_id", columnList = "parent_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_folders_user_parent_name",
                columnNames = {"user_id", "parent_id", "name"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Folder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
