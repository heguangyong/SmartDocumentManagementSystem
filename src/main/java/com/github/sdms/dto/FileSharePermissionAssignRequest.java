package com.github.sdms.dto;

import com.github.sdms.model.enums.PermissionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileSharePermissionAssignRequest {
    @NotNull
    private Long fileId;

    @NotNull
    private Long targetUserId;

    /**
     * 是否继承桶权限
     */
    private boolean inherit;

    /**
     * 仅当inherit=false时生效
     */
    private Set<PermissionType> permissions;

    // getter/setter
}
