package com.clydrive.service;

import com.clydrive.dtos.request.CreateShareRequest;
import com.clydrive.dtos.response.DownloadableFile;
import com.clydrive.dtos.response.ShareResponse;

import java.util.List;

public interface ShareService {

    ShareResponse createShare(Long fileId, CreateShareRequest request);

    List<ShareResponse> listShares(Long fileId);

    void revokeShare(String token);

    DownloadableFile accessSharedFile(String token, String password);
}
