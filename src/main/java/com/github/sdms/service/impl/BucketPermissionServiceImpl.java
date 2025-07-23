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
    public boolean canRead(String uid, Long bucketId) {
        boolean result = bucketPermissionRepository.hasPermission(uid, bucketId, "read")
                || bucketPermissionRepository.hasPermission(uid, bucketId, "admin");
        log.debug("权限校验 - 用户 [{}] 对桶 [{}] 的读取权限: {}", uid, bucketId, result);
        return result;
    }

    @Override
    public boolean canWrite(String uid, Long bucketId) {
        boolean result = bucketPermissionRepository.hasPermission(uid, bucketId, "write")
                || bucketPermissionRepository.hasPermission(uid, bucketId, "admin");
        log.debug("权限校验 - 用户 [{}] 对桶 [{}] 的写入权限: {}", uid, bucketId, result);
        return result;
    }

    @Override
    public List<Long> getAccessibleBucketIds(String uid) {
        return bucketPermissionRepository.findAllByUid(uid)
                .stream()
                .map(BucketPermission::getBucketId)
                .distinct()
                .toList();
    }

}
