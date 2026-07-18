package com.clydrive.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonPropertyOrder({"storageUsed", "storageQuota", "storageAvailable", "usagePercentage"})
public class StorageResponse {

    private long storageUsed;
    private long storageQuota;
    private long storageAvailable;
    private double usagePercentage;
}
