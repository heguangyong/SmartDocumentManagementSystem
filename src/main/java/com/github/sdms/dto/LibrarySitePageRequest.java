package com.github.sdms.dto;

import lombok.Data;

@Data
public class LibrarySitePageRequest {
    private int page = 0;
    private int size = 10;
    private String keyword; // 支持对 name 和 code 模糊搜索
    private Boolean status; // 启用状态（可选）
}

