package com.github.sdms.dto;

import lombok.Data;

@Data
public class RolePermissionDTO {
    private Long resourceId;
    private String permission;
}
