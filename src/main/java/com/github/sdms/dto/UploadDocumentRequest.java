package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UploadDocumentRequest {

    @Schema(description = "备注信息", example = "会议纪要")
    private String notes;

    @Schema(description = "目标文件夹ID", example = "1001")
    private Long folderId;

    @Schema(description = "目标存储桶ID", example = "2001")
    private Long bucketId;
}

