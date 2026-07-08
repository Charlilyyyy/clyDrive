package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutResponse {

    private boolean loggedOut;
    private boolean tokenRevoked;
}
