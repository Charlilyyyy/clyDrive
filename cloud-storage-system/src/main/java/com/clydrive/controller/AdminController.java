package com.clydrive.controller;

import com.clydrive.dtos.request.UpdateUserRoleRequest;
import com.clydrive.dtos.response.AdminStatsResponse;
import com.clydrive.dtos.response.AdminUserResponse;
import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.LockUserResponse;
import com.clydrive.dtos.response.UnlockUserResponse;
import com.clydrive.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats(HttpServletRequest request) {
        log.info("[ADMIN_STATS] Request received | uri={}", request.getRequestURI());
        AdminStatsResponse response = adminService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Admin stats fetched successfully", request.getRequestURI(), response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers(HttpServletRequest request) {
        log.info("[ADMIN_GET_ALL_USERS] Request received | uri={}", request.getRequestURI());
        List<AdminUserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Users fetched successfully", request.getRequestURI(), users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_GET_USER] Request received | userId={} | uri={}", id, request.getRequestURI());
        AdminUserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User fetched successfully", request.getRequestURI(), user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            HttpServletRequest servletRequest) {
        log.info("[ADMIN_UPDATE_ROLE] Request received | userId={} | role={}", id, request.getRole());
        AdminUserResponse response = adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User role updated successfully", servletRequest.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<ApiResponse<LockUserResponse>> lockUser(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_LOCK_USER] Request received | userId={}", id);
        LockUserResponse response = adminService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User locked successfully", request.getRequestURI(), response));
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<UnlockUserResponse>> unlockUser(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_UNLOCK_USER] Request received | userId={}", id);
        UnlockUserResponse response = adminService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User unlocked successfully", request.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> enableUser(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_ENABLE_USER] Request received | userId={}", id);
        AdminUserResponse response = adminService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User enabled successfully", request.getRequestURI(), response));
    }

    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> disableUser(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_DISABLE_USER] Request received | userId={}", id);
        AdminUserResponse response = adminService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User disabled successfully", request.getRequestURI(), response));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deleteUser(
            @PathVariable Long id, HttpServletRequest request) {
        log.info("[ADMIN_DELETE_USER] Request received | userId={}", id);
        AdminUserResponse response = adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "User deleted successfully", request.getRequestURI(), response));
    }
}
