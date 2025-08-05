package com.github.sdms.dto;

import lombok.Data;

/**
 * 管理员设置指定桶的某用户权限（VIEW / EDIT）
 */
@Data
public class AssignBucketPermissionRequest {
    private Long userId;         // user.id，系统内部主键
    private Long bucketId;       // 桶 ID
    private String permission;   // VIEW / EDIT，前端传值，必须是权限字符串
}
