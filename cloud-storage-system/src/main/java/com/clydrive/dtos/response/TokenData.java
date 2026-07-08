package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenData {

    private String accessToken;
    private String refreshToken;
}
