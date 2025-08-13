package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "移动文件或文件夹请求参数")
public class MoveItemRequest {

    @Schema(description = "文件ID列表，可为空或空列表", example = "[101, 102, 103]")
    private List<Long> fileIds;

    @Schema(description = "文件夹ID列表，可为空或空列表", example = "[201, 202]")
    private List<Long> folderIds;

    @Schema(description = "目标父目录ID", example = "301")
    private Long targetFolderId;
}

