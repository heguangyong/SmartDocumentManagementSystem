package com.github.sdms.dto;

import lombok.Data;

@Data
public class RolePermissionDTO {
    private Long resourceId;  // 资源 ID
    private String permission;  // 权限类型，如 READ, WRITE, DELETE
}
