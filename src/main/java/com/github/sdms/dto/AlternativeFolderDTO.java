package com.github.sdms.dto;

import com.github.sdms.model.Folder;
import lombok.Data;

/**
 * 备选文件夹 DTO
 */
@Data
public class AlternativeFolderDTO {
    private Long id;
    private String name;
    private Long parentId;
    private String path; // 完整路径，用于显示层级关系
    private Integer level; // 层级深度

    public AlternativeFolderDTO(Folder folder, String path, Integer level) {
        this.id = folder.getId();
        this.name = folder.getName();
        this.parentId = folder.getParentId();
        this.path = path;
        this.level = level;
    }

    // 无参构造函数
    public AlternativeFolderDTO() {
    }
}
