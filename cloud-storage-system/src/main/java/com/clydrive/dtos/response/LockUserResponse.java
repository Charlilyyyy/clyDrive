package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LockUserResponse {

    private Long userId;
    private String username;
    private String email;
    private boolean accountNonLocked;
    private LocalDateTime lockedUntil;
    private LocalDateTime lockedAt;
}
