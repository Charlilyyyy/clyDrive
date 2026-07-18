package com.clydrive.controller;

import com.clydrive.dtos.response.ApiResponse;
import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.FileResponse;
import com.clydrive.dtos.response.PageResponse;
import com.clydrive.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpServletRequest request) {

        log.info("[FILE_UPLOAD] Request received | folderId={} | uri={}", folderId, request.getRequestURI());
        FileResponse response = fileService.upload(file, folderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(), "File uploaded successfully", request.getRequestURI(), response));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        log.info("[FILE_DOWNLOAD] Request received | fileId={}", id);
        DownloadableFile file = fileService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getResource());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FileResponse>>> listFiles(
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        PageResponse<FileResponse> response = fileService.listFiles(folderId, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Files fetched successfully", request.getRequestURI(), response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(@PathVariable Long id, HttpServletRequest request) {
        FileResponse response = fileService.getFile(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "File fetched successfully", request.getRequestURI(), response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        fileService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "File deleted successfully", request.getRequestURI(), null));
    }
}
