package com.clydrive.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonPropertyOrder({
        "totalUsers",
        "activeUsers",
        "pendingUsers",
        "lockedUsers",
        "disabledUsers",
        "deletedUsers",
        "totalStorageUsed"
})
public class AdminStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long pendingUsers;
    private long lockedUsers;
    private long disabledUsers;
    private long deletedUsers;
    private long totalStorageUsed;
}
