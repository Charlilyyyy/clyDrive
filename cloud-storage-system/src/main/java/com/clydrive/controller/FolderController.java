package com.clydrive.controller;

import com.clydrive.dtos.request.CreateFolderRequest;
import com.clydrive.dtos.request.RenameFolderRequest;
import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.FolderContentsResponse;
import com.clydrive.dtos.response.FolderResponse;
import com.clydrive.service.FolderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/folders")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request, HttpServletRequest servletRequest) {
        log.info("[FOLDER_CREATE] Request received | name={} | parentId={}", request.getName(), request.getParentId());
        FolderResponse response = folderService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(), "Folder created successfully", servletRequest.getRequestURI(), response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> listRootFolders(HttpServletRequest request) {
        List<FolderResponse> folders = folderService.listRootFolders();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Root folders fetched successfully", request.getRequestURI(), folders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FolderContentsResponse>> getFolderContents(
            @PathVariable Long id, HttpServletRequest request) {
        FolderContentsResponse response = folderService.getFolderContents(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Folder contents fetched successfully", request.getRequestURI(), response));
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<FolderResponse>> renameFolder(
            @PathVariable Long id,
            @Valid @RequestBody RenameFolderRequest request,
            HttpServletRequest servletRequest) {
        FolderResponse response = folderService.renameFolder(id, request.getName());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Folder renamed successfully", servletRequest.getRequestURI(), response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(@PathVariable Long id, HttpServletRequest request) {
        folderService.deleteFolder(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Folder deleted successfully", request.getRequestURI(), null));
    }
}
