package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailVerificationResponse {

    private boolean verified;
    private String message;
}
