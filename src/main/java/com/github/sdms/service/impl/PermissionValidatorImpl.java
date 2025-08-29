package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.repository.BucketRepository;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.FilePermissionService;
import com.github.sdms.service.PermissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionValidatorImpl implements PermissionValidator {
    @Autowired
    private  UserRepository userRepository;
    @Autowired
    private  FilePermissionService filePermissionService;
    @Autowired
    private BucketPermissionRepository bucketPermissionRepository;
    @Autowired
    private BucketRepository bucketRepository;

    @Override
    public boolean canReadBucket(Long userId, String bucketName) {
        User user = findUserOrThrow(userId);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            Long bucketId = findBucketIdByName(bucketName);
            if (bucketId == null) return false; // 存储桶不存在，无权限
            return bucketPermissionRepository.findByUserIdAndBucketId(userId, bucketId)
                    .map(permission -> {
                        String perms = permission.getPermission();
                        return perms != null && Arrays.stream(perms.split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .anyMatch(p -> p.equals("read") || p.equals("admin"));
                    })
                    .orElse(false);
        }
        return isOwnBucket(userId, bucketName);
    }


    @Override
    public boolean canWriteBucket(Long userId, String bucketName) {
        User user = findUserOrThrow(userId);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            Long bucketId = findBucketIdByName(bucketName);
            if (bucketId == null) return false; // 存储桶不存在，无权限
            return bucketPermissionRepository.findByUserIdAndBucketId(userId, bucketId)
                    .map(permission -> {
                        String perms = permission.getPermission();
                        if (perms == null) return false;

                        return Arrays.stream(perms.split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .anyMatch(p -> p.contains("write") || p.contains("admin"));
                    })
                    .orElse(false);

        }
        return isOwnBucket(userId, bucketName);
    }


    @Override
    public boolean canReadFile(Long userId, Long fileId) {
        return filePermissionService.checkUserPermission(userId, fileId, PermissionType.READ);
    }

    @Override
    public boolean canWriteFile(Long userId, Long fileId) {
        return filePermissionService.checkUserPermission(userId, fileId, PermissionType.WRITE);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return findUserOrThrow(userId).getRoleType() == RoleType.ADMIN;
    }

    @Override
    public boolean isLibrarian(Long userId) {
        return findUserOrThrow(userId).getRoleType() == RoleType.LIBRARIAN;
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + userId));
    }

    private boolean isOwnBucket(Long userId, String bucketName) {
        return bucketRepository.findByName(bucketName)
                .map(bucket -> Objects.equals(bucket.getOwnerId(), userId))
                .orElse(false);
    }

    private String extractLibraryCode(String bucketId) {
        String[] parts = bucketId.split("-");
        if (parts.length >= 3) return parts[2];
        throw new ApiException("提取桶名中的LibraryCode失败: " + bucketId);
    }

    @Override
    public boolean hasWritePermission(Long userId, String bucketName) {
        User user = findUserOrThrow(userId);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            Long bucketId = findBucketIdByName(bucketName);
            if (bucketId == null) return false; // 存储桶不存在，无权限
            return bucketPermissionRepository.findByUserIdAndBucketId(userId, bucketId)
                    .map(permission -> {
                        String perms = permission.getPermission();
                        if (perms == null) return false;

                        return Arrays.stream(perms.split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .anyMatch(p -> p.contains("write") || p.contains("admin"));
                    })
                    .orElse(false);

        }
        return isOwnBucket(userId, bucketName);
    }

    private Long findBucketIdByName(String bucketName) {
        return bucketRepository.findByName(bucketName)
                .map(Bucket::getId)
                .orElse(null);
    }
}
