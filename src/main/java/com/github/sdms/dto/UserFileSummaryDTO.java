package com.github.sdms.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserFileSummaryDTO {
    private Long id;
    private String originFilename;  // 文件名
    private String type;            // 类型
    private Long size;              // 文件大小（byte）
    private Date createdDate;       // 上传时间
    private Integer versionNumber;  // 版本号
    private Boolean isLatest;       // 是否最新版本
    private Boolean shared;         // 是否共享
    private Long folderId;          // 所属目录ID

}
