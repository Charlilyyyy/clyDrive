package com.clydrive.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "message", "path", "errors", "timestamp"})
public class ErrorResponse {

    private int status;
    private String message;
    private String path;
    private Object errors;
    private LocalDateTime timestamp;
}
