package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "移动文件夹请求对象")
public class MoveRequest {
    @Schema(description = "要移动的文件ID列表")
    private List<Long> fileIds;

    @Schema(description = "要移动的文件夹ID列表")
    private List<Long> folderIds;

    @Schema(description = "目标文件夹ID，null表示移动到根目录")
    private Long targetFolderId;

    // 但如果需要空集合保护，可以保留这些方法
    public List<Long> getFileIds() {
        return fileIds != null ? fileIds : new ArrayList<>();
    }

    public List<Long> getFolderIds() {
        return folderIds != null ? folderIds : new ArrayList<>();
    }
}