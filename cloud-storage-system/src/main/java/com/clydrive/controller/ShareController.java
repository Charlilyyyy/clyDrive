package com.clydrive.controller;

import com.clydrive.dtos.request.CreateShareRequest;
import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.ShareResponse;
import com.clydrive.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/api/v1/files/{fileId}/share")
    public ResponseEntity<ApiResponse<ShareResponse>> createShare(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareRequest request,
            HttpServletRequest servletRequest) {
        log.info("[SHARE_CREATE] Request received | fileId={}", fileId);
        ShareResponse response = shareService.createShare(fileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(), "Share link created successfully", servletRequest.getRequestURI(), response));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/api/v1/files/{fileId}/shares")
    public ResponseEntity<ApiResponse<List<ShareResponse>>> listShares(
            @PathVariable Long fileId, HttpServletRequest request) {
        List<ShareResponse> shares = shareService.listShares(fileId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Shares fetched successfully", request.getRequestURI(), shares));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/api/v1/share/{token}")
    public ResponseEntity<ApiResponse<Void>> revokeShare(
            @PathVariable String token, HttpServletRequest request) {
        log.info("[SHARE_REVOKE] Request received");
        shareService.revokeShare(token);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Share link revoked successfully", request.getRequestURI(), null));
    }

    @GetMapping("/api/v1/share/{token}")
    public ResponseEntity<Resource> accessShare(
            @PathVariable String token,
            @RequestParam(value = "password", required = false) String password) {
        log.info("[SHARE_DOWNLOAD] Public access request received");
        DownloadableFile file = shareService.accessSharedFile(token, password);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getResource());
    }
}
