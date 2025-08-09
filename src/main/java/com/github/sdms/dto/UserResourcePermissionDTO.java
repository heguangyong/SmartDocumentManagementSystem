package com.github.sdms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResourcePermissionDTO {

    private Long resourceId;

    private String resourceType; // e.g., "BUCKET", "FILE"

    private String resourceName;

    private boolean canRead;

    private boolean canWrite;

    private boolean canDelete;

    /**
     * 权限来源，示例："ROLE" 或 "CUSTOM"
     */
    private String permissionSource;
}
