package com.github.sdms.service;

public interface PermissionValidator {

    boolean canReadBucket(Long userId, String bucketId);

    boolean canWriteBucket(Long userId, String bucketId);

    boolean canReadFile(Long userId, Long fileId);

    boolean canWriteFile(Long userId, Long fileId);

    boolean isAdmin(Long userId);

    boolean isLibrarian(Long userId);

    boolean hasWritePermission(Long userId, String bucketId);

}