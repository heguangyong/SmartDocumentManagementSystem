package com.github.sdms.dto;

import com.github.sdms.model.enums.BucketAction;
import com.github.sdms.model.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BucketUserPermissionDTO {

    @Schema(description = "用户表ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户角色")
    private RoleType roleType;

    @Schema(description = "权限类型（READ, WRITE, DELETE, MANAGE）")
    private List<BucketAction> permissions;

    @Schema(description = "更新时间")
    private Date updatedAt;
}
