package com.clydrive.service;

import com.clydrive.dtos.request.UserRegistrationRequest;
import com.clydrive.dtos.response.UserResponse;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest request);
}
