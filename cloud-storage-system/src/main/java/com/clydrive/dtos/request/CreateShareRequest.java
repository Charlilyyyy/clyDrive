package com.clydrive.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateShareRequest {

    /**
     * Number of hours until the link expires. Defaults to 24 when not provided.
     */
    @Min(value = 1, message = "Expiry must be at least 1 hour")
    @Max(value = 8760, message = "Expiry cannot exceed 1 year")
    private Integer expiryHours;

    /**
     * Optional password required to access the shared file.
     */
    private String password;
}
