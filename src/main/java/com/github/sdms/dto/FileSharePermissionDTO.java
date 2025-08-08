package com.github.sdms.dto;

import com.github.sdms.model.enums.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileSharePermissionDTO {
    private Long fileId;
    private Long targetUserId;
    private String targetUsername;

    /**
     * 权限类型枚举：READ, WRITE, DELETE
     */
    private Set<PermissionType> permissions;

    /**
     * 是否继承自桶权限
     */
    private boolean inherited;

    // getter/setter
}
