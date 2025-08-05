package com.github.sdms.dto;

import lombok.Data;

@Data
public class BucketPermissionDTO {
    private Long bucketId;      // 存储桶ID
    private Long userId;        // 用户ID，对应User表的主键ID
    private String permission;  // 权限类型（READ, WRITE, DELETE）
}
