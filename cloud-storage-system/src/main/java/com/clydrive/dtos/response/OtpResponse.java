package com.clydrive.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonPropertyOrder({"email", "expiryMinutes", "emailSent"})
public class OtpResponse {

    private String email;
    private int expiryMinutes;
    private boolean emailSent;
}
