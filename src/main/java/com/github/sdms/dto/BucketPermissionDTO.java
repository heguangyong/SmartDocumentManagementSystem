package com.github.sdms.dto;

import lombok.Data;

@Data
public class BucketPermissionDTO {
    private Long bucketId;      // 存储桶ID
    private String uid;         // 用户ID (可以直接使用uid进行查询)
    private String permission;  // 权限类型（READ, WRITE, DELETE）
}
