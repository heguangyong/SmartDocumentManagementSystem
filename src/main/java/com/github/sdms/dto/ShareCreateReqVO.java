package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建分享请求对象")
public class ShareCreateReqVO {

    @Schema(description = "分享目标类型，支持 'file' 或 'folder'", requiredMode = Schema.RequiredMode.REQUIRED, example = "file")
    private String type;

    @Schema(description = "分享目标ID（文件ID或目录ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    private Long targetId;

    @Schema(description = "分享链接过期时间，不传默认7天后", example = "2025-08-20T12:00:00")
    private LocalDateTime expireAt;

}
