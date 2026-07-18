package com.clydrive.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Persist the given upload and return the relative storage path (key).
     */
    String store(MultipartFile file, Long userId, String storedFileName);

    /**
     * Load a previously stored file as a readable resource.
     */
    Resource load(String storagePath);

    /**
     * Remove a stored file. Missing files are ignored.
     */
    void delete(String storagePath);
}
