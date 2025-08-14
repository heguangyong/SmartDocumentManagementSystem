package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserFileVersionRequest {

    @NotNull
    @Schema(description = "文档 ID", example = "123")
    private Long docId;

    @Schema(description = "桶 ID，可选", example = "5")
    private Long bucketId;

    @Schema(description = "目录 ID，可选", example = "10")
    private Long folderId;
}

