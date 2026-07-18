package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FolderResponse {

    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
}
