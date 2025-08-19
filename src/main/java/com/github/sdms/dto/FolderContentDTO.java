package com.github.sdms.dto;

import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import lombok.Data;

import java.util.Date;

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

    // 构造函数 - 用于文件夹
    public FolderContentDTO(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();
        this.type = "folder";
        this.createdDate = folder.getCreatedDate();
        this.updatedDate = folder.getUpdatedDate();
        this.isPublic = folder.getIsPublic();
        this.shared = folder.getShared();
    }

    // 构造函数 - 用于文件
    public FolderContentDTO(UserFile file) {
        this.id = file.getId();
        this.name = file.getOriginFilename();
        this.type = "file";
        this.size = file.getSize();
        this.createdDate = file.getCreatedDate();
        this.originFilename = file.getOriginFilename();
        this.fileType = file.getType();
    }
}
