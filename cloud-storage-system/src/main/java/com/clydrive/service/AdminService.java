package com.clydrive.service;

import com.clydrive.dtos.response.AdminStatsResponse;
import com.clydrive.dtos.response.AdminUserResponse;
import com.clydrive.dtos.response.LockUserResponse;
import com.clydrive.dtos.response.UnlockUserResponse;
import com.clydrive.enums.Role;

import java.util.List;

public interface AdminService {

    AdminStatsResponse getAdminStats();

    List<AdminUserResponse> getAllUsers();

    AdminUserResponse getUserById(Long userId);

    AdminUserResponse updateUserRole(Long userId, Role role);

    LockUserResponse lockUser(Long userId);

    UnlockUserResponse unlockUser(Long userId);

    AdminUserResponse enableUser(Long userId);

    AdminUserResponse disableUser(Long userId);

    AdminUserResponse deleteUser(Long userId);
}
