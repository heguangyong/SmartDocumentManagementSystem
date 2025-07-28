package com.github.sdms.service;

public interface PermissionValidator {

    boolean canReadBucket(String uid, String bucketId);

    boolean canWriteBucket(String uid, String bucketId);

    boolean canReadFile(String uid, Long fileId);

    boolean canWriteFile(String uid, Long fileId);

    boolean isAdmin(String uid);

    boolean isLibrarian(String uid);

    boolean hasWritePermission(String uid, String bucketName);

}