package com.clydrive.service;

import com.clydrive.dtos.request.CreateFolderRequest;
import com.clydrive.dtos.response.FolderContentsResponse;
import com.clydrive.dtos.response.FolderResponse;

import java.util.List;

public interface FolderService {

    FolderResponse createFolder(CreateFolderRequest request);

    List<FolderResponse> listRootFolders();

    FolderContentsResponse getFolderContents(Long folderId);

    FolderResponse renameFolder(Long folderId, String name);

    void deleteFolder(Long folderId);
}
