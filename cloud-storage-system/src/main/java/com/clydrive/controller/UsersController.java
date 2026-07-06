package com.clydrive.controller;

import com.clydrive.dtos.request.UserRegistrationRequest;
import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.UserResponse;
import com.clydrive.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest servletRequest) {

        log.info("[REGISTER_USER] Request received | username={} | email={} | ip={} | uri={}",
                request.getUsername(), request.getEmail(),
                servletRequest.getRemoteAddr(), servletRequest.getRequestURI());

        UserResponse response = userService.registerUser(request);

        log.info("[REGISTER_USER] Registration completed | userId={} | username={}",
                response.getId(), response.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "User registered successfully",
                        servletRequest.getRequestURI(),
                        response
                )
        );
    }
}
