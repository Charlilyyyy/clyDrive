package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChangePasswordResponse {

    private boolean passwordChanged;
    private String message;
}
