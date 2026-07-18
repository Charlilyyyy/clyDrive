package com.clydrive.service.impl;

import com.clydrive.exception.FileStorageException;
import com.clydrive.exception.ResourceNotFoundException;
import com.clydrive.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${storage.root}") String root) {
        this.rootLocation = Paths.get(root).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("[STORAGE] Root initialized | path={}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file, Long userId, String storedFileName) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store an empty file");
        }

        try {
            Path userDir = rootLocation.resolve(String.valueOf(userId)).normalize();
            if (!userDir.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid storage path");
            }
            Files.createDirectories(userDir);

            Path destination = userDir.resolve(storedFileName).normalize();
            if (!destination.startsWith(userDir)) {
                throw new FileStorageException("Invalid storage path");
            }

            try (var input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = rootLocation.relativize(destination).toString();
            log.info("[STORAGE] Stored file | userId={} | path={}", userId, relativePath);
            return relativePath;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    @Override
    public Resource load(String storagePath) {
        try {
            Path file = rootLocation.resolve(storagePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid storage path");
            }

            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found on storage");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new FileStorageException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path file = rootLocation.resolve(storagePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid storage path");
            }
            boolean deleted = Files.deleteIfExists(file);
            log.info("[STORAGE] Delete file | path={} | deleted={}", storagePath, deleted);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }
}
