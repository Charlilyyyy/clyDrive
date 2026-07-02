package com.clydrive.controller;

import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.HealthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> health(HttpServletRequest request) {
        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .application(applicationName)
                .version("0.0.1-SNAPSHOT")
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Service is healthy",
                        request.getRequestURI(),
                        health
                )
        );
    }
}
