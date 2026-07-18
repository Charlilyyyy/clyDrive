package com.clydrive.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateQuotaRequest {

    @NotNull(message = "Storage quota is required")
    @Min(value = 0, message = "Storage quota cannot be negative")
    private Long storageQuota;
}
