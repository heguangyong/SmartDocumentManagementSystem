package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LibrarySitePageRequest {

    @Schema(description = "页码，从 0 开始", example = "0")
    private int page = 0;

    @Schema(description = "每页数量", example = "10")
    private int size = 10;

    @Schema(description = "支持对 name 和 code 模糊搜索", example = "中心馆")
    private String keyword;

    @Schema(description = "启用状态（true 表示启用，false 表示停用）", example = "true")
    private Boolean status;
}
