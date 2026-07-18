package com.clydrive.dtos.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class FolderContentsResponse {

    private Long folderId;
    private String folderName;
    private List<BreadcrumbItem> breadcrumbs;
    private List<FolderResponse> subFolders;
    private List<FileResponse> files;
}
