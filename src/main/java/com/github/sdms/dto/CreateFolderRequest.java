package com.github.sdms.dto;

// 1. 创建请求对象
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFolderRequest {

    @NotBlank
    @Schema(description = "文件夹名称", required = true)
    private String name;

    @Schema(description = "父文件夹ID，可为空表示根目录")
    private Long parentId;

    @NotNull
    @Schema(description = "存储桶ID，用于绑定文件夹所属存储桶", required = true)
    private Long bucketId;
}
