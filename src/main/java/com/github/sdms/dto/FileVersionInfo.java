package com.github.sdms.dto;

import com.github.sdms.model.UserFile;
import lombok.Data;

import java.util.Date;

@Data
public class FileVersionInfo {
    private Long fileId;               // 文件记录ID (UserFile表主键)
    private Long docId;                // 文档组ID
    private Integer versionNumber;     // 版本号
    private String notes;              // 版本说明
    private Date createdDate;          // 创建时间
    private Long size;                 // 文件大小
    private Boolean isLatest;          // 是否最新版本
    private String versionKey;         // OnlyOffice版本Key

    public FileVersionInfo() {

    }

    // 新增构造函数，从UserFile创建版本信息
    public FileVersionInfo(UserFile file) {
        this.fileId = file.getId();
        this.docId = file.getDocId();
        this.versionNumber = file.getVersionNumber();
        this.notes = file.getNotes();
        this.createdDate = file.getCreatedDate();
        this.size = file.getSize();
        this.isLatest = file.getIsLatest();
        this.versionKey = file.getVersionKey();
    }
}