package com.github.sdms.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UserFileSummaryDTO {
    private Long id;                    // 当前显示版本的fileId
    private String originFilename;      // 文件名
    private String type;                // 类型
    private Long size;                  // 文件大小（byte）
    private Date createdDate;          // 上传时间
    private Integer versionNumber;      // 当前显示的版本号
    private Boolean isLatest;           // 是否最新版本
    private Boolean shared;             // 是否共享
    private Long folderId;             // 所属目录ID

    // 新增字段
    private Long docId;                 // 文档组ID
    private List<FileVersionInfo> versions; // 版本列表
    private Integer totalVersions;      // 总版本数
}
