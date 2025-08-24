package com.github.sdms.dto;

import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 创建分层内容 DTO
 */
@Data
public class FolderContentDTO {
    private Long id;
    private String name;
    private String type; // "folder" 或 "file"
    private Long size; // 文件大小，文件夹为 null
    private Date createdDate;
    private Date updatedDate;
    private String originFilename; // 仅文件有效
    private String fileType; // 仅文件有效
    private Boolean isPublic; // 仅文件夹有效
    private Boolean shared; // 仅文件夹有效
    private Long folderId; // 新增字段：父文件夹ID（文件为所在文件夹，文件夹为其父文件夹）

    // 新增版本相关字段
    private Long docId; // 文档ID（仅文件有效）
    private Integer versionNumber; // 当前显示版本号（仅文件有效）
    private Integer totalVersions; // 总版本数（仅文件有效）
    private List<FileVersionInfo> versions; // 所有版本信息（仅文件有效）

    // 构造函数 - 用于文件夹
    public FolderContentDTO(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();
        this.type = "folder";
        this.createdDate = folder.getCreatedDate();
        this.updatedDate = folder.getUpdatedDate();
        this.isPublic = folder.getIsPublic();
        this.shared = folder.getShared();
        this.folderId = folder.getParentId(); // 父文件夹ID
    }

    // 构造函数 - 用于文件（单版本，兼容旧逻辑）
    public FolderContentDTO(UserFile file) {
        this.id = file.getId();
        this.name = file.getOriginFilename();
        this.type = "file";
        this.size = file.getSize();
        this.createdDate = file.getCreatedDate();
        this.originFilename = file.getOriginFilename();
        this.fileType = file.getType();
        this.folderId = file.getFolderId(); // 文件所在文件夹ID
        this.docId = file.getDocId();
        this.versionNumber = file.getVersionNumber();
        this.totalVersions = 1;

        // 创建单个版本信息
        if (file.getDocId() != null) {
            this.versions = Collections.singletonList(new FileVersionInfo(file));
        }
    }

    // 新增构造函数 - 用于文件（多版本支持）
    public FolderContentDTO(UserFile latestFile, List<UserFile> allVersions) {
        this.id = latestFile.getId(); // 使用最新版本的文件ID
        this.name = latestFile.getOriginFilename();
        this.type = "file";
        this.size = latestFile.getSize();
        this.createdDate = latestFile.getCreatedDate();
        this.originFilename = latestFile.getOriginFilename();
        this.fileType = latestFile.getType();
        this.folderId = latestFile.getFolderId();
        this.docId = latestFile.getDocId();
        this.versionNumber = latestFile.getVersionNumber();
        this.totalVersions = allVersions.size();

        // 构建所有版本信息
        this.versions = allVersions.stream()
                .map(FileVersionInfo::new)
                .collect(Collectors.toList());
    }
}
