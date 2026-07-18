package com.clydrive.dtos.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveFileRequest {

    /**
     * Target folder id. A null value moves the file to the user's root.
     */
    private Long folderId;
}
