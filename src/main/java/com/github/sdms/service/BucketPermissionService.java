package com.github.sdms.service;

import java.util.List;

public interface BucketPermissionService {
    boolean canRead(Long userId, Long bucketId);
    boolean canWrite(Long userId, Long bucketId);
    List<Long> getAccessibleBucketIds(Long userId);

}

