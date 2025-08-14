package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserFilePageRequest {

    @Schema(description = "页码，从 0 开始", example = "0")
    private int page = 0;

    @Schema(description = "每页数量", example = "10")
    private int size = 10;

    @Schema(description = "文件或目录名模糊搜索", example = "报告")
    private String name;

    @Schema(description = "文件类型过滤", example = "pdf")
    private String type;

    @Schema(description = "所属目录 ID", example = "123")
    private Long folderId;

    @Schema(description = "馆代码", example = "LIB001")
    private String libraryCode;

    @Schema(description = "支持文件名/目录名的模糊搜索关键字", example = "会议纪要")
    private String keyword;

    @Schema(description = "桶 ID 查询条件", example = "5")
    private Long bucketId;
}
