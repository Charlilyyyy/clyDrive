package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResetPasswordResponse {

    private boolean passwordUpdated;
    private boolean tokensRevoked;
}
