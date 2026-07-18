package com.clydrive.service;

import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.FileResponse;
import com.clydrive.dtos.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileResponse upload(MultipartFile file, Long folderId);

    DownloadableFile download(Long fileId);

    PageResponse<FileResponse> listFiles(Long folderId, Pageable pageable);

    FileResponse getFile(Long fileId);

    void deleteFile(Long fileId);
}
