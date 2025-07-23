package com.github.sdms.service;

import java.util.List;

public interface BucketPermissionService {
    boolean canRead(String uid, Long bucketId);
    boolean canWrite(String uid, Long bucketId);
    List<Long> getAccessibleBucketIds(String uid);

}

