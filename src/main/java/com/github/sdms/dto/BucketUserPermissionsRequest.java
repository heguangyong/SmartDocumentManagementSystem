package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "桶用户权限分配请求")
public class BucketUserPermissionsRequest {

    @Schema(description = "存储桶ID", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bucketId;

    @Schema(description = "用户权限列表")
    private List<UserPermissionDTO> userPermissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户权限定义")
    public static class UserPermissionDTO {

        @Schema(description = "用户ID", example = "1900", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;

        @ArraySchema(
                schema = @Schema(description = "权限枚举值", example = "READ"),
                arraySchema = @Schema(
                        description = "权限集合，可选值: READ, WRITE, DELETE, MANAGE"
                )
        )
        private List<String> permissions;
    }
}
