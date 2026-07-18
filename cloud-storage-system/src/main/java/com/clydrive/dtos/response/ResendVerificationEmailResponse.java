package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResendVerificationEmailResponse {

    private boolean emailSent;
    private String email;
    private int expiryMinutes;
}
