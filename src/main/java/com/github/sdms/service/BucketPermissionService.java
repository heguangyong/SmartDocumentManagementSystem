package com.github.sdms.service;

public interface BucketPermissionService {
    boolean canRead(String uid, Long bucketId);
    boolean canWrite(String uid, Long bucketId);
}

