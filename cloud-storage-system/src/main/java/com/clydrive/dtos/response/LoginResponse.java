package com.clydrive.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"username", "fullName", "email", "phoneNumber", "role", "tokens"})
public class LoginResponse {

    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private TokenData tokens;
}
