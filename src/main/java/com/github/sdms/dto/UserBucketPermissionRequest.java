package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个用户桶权限分配请求
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBucketPermissionRequest {
    private Long bucketId;
    private Long userId;
    private List<String> permissions; // ["READ", "WRITE", "DELETE", "MANAGE"]
}

