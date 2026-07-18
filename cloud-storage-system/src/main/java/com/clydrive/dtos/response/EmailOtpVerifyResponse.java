package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailOtpVerifyResponse {

    private boolean verified;
    private String email;
    private boolean canResetPassword;
}
