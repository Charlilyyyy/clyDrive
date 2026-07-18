package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@Builder
public class DownloadableFile {

    private Resource resource;
    private String fileName;
    private String contentType;
    private long size;
}
