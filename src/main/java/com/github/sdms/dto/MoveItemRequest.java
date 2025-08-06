package com.github.sdms.dto;

import lombok.Data;

import java.util.List;

@Data
public class MoveItemRequest {
    private List<Long> fileIds;       // 可为 null 或空列表
    private List<Long> folderIds;     // 新增字段，支持目录
    private Long targetFolderId;      // 目标父目录 ID
}
