package com.clydrive.dtos.response;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminUserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private boolean enabled;
    private boolean emailVerified;
    private boolean accountNonLocked;
    private long storageUsed;
    private long storageQuota;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
