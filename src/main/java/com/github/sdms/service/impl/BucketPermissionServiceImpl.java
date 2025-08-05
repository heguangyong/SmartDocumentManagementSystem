package com.github.sdms.service.impl;

import com.github.sdms.model.BucketPermission;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.service.BucketPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BucketPermissionServiceImpl implements BucketPermissionService {

    private final BucketPermissionRepository bucketPermissionRepository;

    public BucketPermissionServiceImpl(BucketPermissionRepository bucketPermissionRepository) {
        this.bucketPermissionRepository = bucketPermissionRepository;
    }

    @Override
    public boolean canRead(Long userId, Long bucketId) {
        boolean result = bucketPermissionRepository.hasPermission(userId, bucketId, "read")
                || bucketPermissionRepository.hasPermission(userId, bucketId, "admin");
        log.debug("权限校验 - 用户 [{}] 对桶 [{}] 的读取权限: {}", userId, bucketId, result);
        return result;
    }

    @Override
    public boolean canWrite(Long userId, Long bucketId) {
        boolean result = bucketPermissionRepository.hasPermission(userId, bucketId, "write")
                || bucketPermissionRepository.hasPermission(userId, bucketId, "admin");
        log.debug("权限校验 - 用户 [{}] 对桶 [{}] 的写入权限: {}", userId, bucketId, result);
        return result;
    }

    @Override
    public List<Long> getAccessibleBucketIds(Long userId) {
        return bucketPermissionRepository.findAllByUserId(userId)
                .stream()
                .map(BucketPermission::getBucketId)
                .distinct()
                .toList();
    }

}
