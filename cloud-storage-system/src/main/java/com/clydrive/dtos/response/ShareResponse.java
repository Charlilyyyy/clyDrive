package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ShareResponse {

    private Long id;
    private Long fileId;
    private String token;
    private boolean passwordProtected;
    private long downloadCount;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;
}
