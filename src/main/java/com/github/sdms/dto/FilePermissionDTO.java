package com.github.sdms.dto;

import com.github.sdms.model.enums.PermissionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilePermissionDTO {
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long fileId;

    @NotNull
    private PermissionType permission;

}
