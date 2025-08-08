package com.github.sdms.dto;

import com.github.sdms.model.enums.PermissionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilePermissionUpdateRequest {
    @NotNull
    private Long id; // 权限记录ID

    @NotNull
    private PermissionType permission;
}
