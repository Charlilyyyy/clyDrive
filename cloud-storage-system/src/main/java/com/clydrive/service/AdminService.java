package com.clydrive.service;

import com.clydrive.dtos.response.AdminStatsResponse;
import com.clydrive.dtos.response.AdminUserResponse;

import java.util.List;

public interface AdminService {

    AdminStatsResponse getAdminStats();

    List<AdminUserResponse> getAllUsers();

    AdminUserResponse getUserById(Long userId);
}
