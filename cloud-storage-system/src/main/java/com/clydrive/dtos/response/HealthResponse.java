package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthResponse {

    private String status;
    private String application;
    private String version;
}
