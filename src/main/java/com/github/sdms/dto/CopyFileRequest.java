package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CopyFileRequest {

    @Schema(description = "要复制的文件 ID", example = "1001")
    @NotNull(message = "文件 ID 不能为空")
    private Long fileId;

    @Schema(description = "目标目录 ID", example = "2002")
    @NotNull(message = "目标目录 ID 不能为空")
    private Long targetFolderId;
}
