package com.github.sdms.dto;

import com.github.sdms.model.enums.PermissionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilePermissionAssignRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long fileId;

    @NotNull
    private PermissionType permission;

    @NotNull
    private String libraryCode;  // 新增
}
