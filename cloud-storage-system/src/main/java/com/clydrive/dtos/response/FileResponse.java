package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FileResponse {

    private Long id;
    private String name;
    private String type;
    private long size;
    private Long folderId;
    private LocalDateTime uploadedAt;
}
