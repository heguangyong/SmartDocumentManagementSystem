package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RemoveBucketPermissionRequest {

    @Schema(description = "桶 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bucketId;

    @Schema(description = "用户表 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
}

