package com.github.sdms.dto;

import com.github.sdms.model.Folder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderDTO {

    private Long id;
    private String name;
    private String libraryCode;
    private Long userId;
    private Long parentId;
    private Boolean isPublic;
    private Boolean systemFolder;
    private Date createdDate;
    private Date updatedDate;
    private Boolean shared;
    private Date shareExpireAt;
    private Long bucketId;

    // 构造方法：通过实体生成 DTO
    public FolderDTO(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();
        this.libraryCode = folder.getLibraryCode();
        this.userId = folder.getUserId();
        this.parentId = folder.getParentId();
        this.isPublic = folder.getIsPublic();
        this.systemFolder = folder.getSystemFolder();
        this.createdDate = folder.getCreatedDate();
        this.updatedDate = folder.getUpdatedDate();
        this.shared = folder.getShared();
        this.shareExpireAt = folder.getShareExpireAt();
        this.bucketId = folder.getBucketId();
    }
}
