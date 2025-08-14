package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class UserFileDTO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "原始文件名")
    private String originFilename;

    @Schema(description = "存储对象名")
    private String name;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "MinIO对象路径")
    private String url;

    @Schema(description = "存储桶ID")
    private Long bucketId;

    @Schema(description = "所属目录ID")
    private Long folderId;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "版本号")
    private Integer versionNumber;

    @Schema(description = "版本说明")
    private String notes;

    @Schema(description = "是否最新版本")
    private Boolean isLatest;

    @Schema(description = "是否共享")
    private Boolean shared;

    @Schema(description = "创建时间")
    private Date createdDate;

    @Schema(description = "馆代码")
    private String libraryCode;  // ✅ 新增
}
