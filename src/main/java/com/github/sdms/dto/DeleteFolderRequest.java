package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteFolderRequest {

    @NotNull
    @Schema(description = "要删除的文件夹ID", required = true)
    private Long folderId;
}