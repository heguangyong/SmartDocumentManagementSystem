package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.repository.BucketRepository;
import com.github.sdms.service.BucketPermissionService;
import com.github.sdms.service.BucketService;
import com.github.sdms.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketServiceImpl implements BucketService {

    private final BucketRepository bucketRepository;
    private final MinioClient minioClient;
    private final MinioService minioService;
    private final BucketPermissionService bucketPermissionService;

    /**
     * 创建存储桶（同时在数据库和 MinIO 创建）
     */
    @Transactional
    @Override
    public Bucket createBucket(Bucket bucket) {
        if (bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket.getName()).build()
                );
                log.info("MinIO 创建桶成功：{}", bucket.getName());
            } else {
                log.warn("MinIO 中桶已存在：{}", bucket.getName());
            }
        } catch (MinioException e) {
            log.error("MinIO 创建桶失败", e);
            throw new ApiException(500, "创建 MinIO 桶失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("未知错误：MinIO 创建桶失败", e);
            throw new ApiException(500, "MinIO 创建桶异常：" + e.getMessage());
        }

        return bucketRepository.save(bucket);
    }

    /**
     * 根据 ID 查询桶（同时校验 MinIO 中是否存在）
     */
    @Override
    public Bucket getBucketById(Long id) {
        Bucket bucket = bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (!exists) {
                throw new ApiException(404, "MinIO 中桶不存在：" + bucket.getName());
            }
        } catch (Exception e) {
            log.error("MinIO 校验桶存在性失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        return bucket;
    }

    /**
     * 获取所有桶（从 MinIO 获取后与数据库比对，只保留存在的）
     */
    @Override
    public List<Bucket> getAllBuckets() {
        List<Bucket> all = bucketRepository.findAll();
        return all.stream().filter(bucket -> {
            try {
                return minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket.getName()).build()
                );
            } catch (Exception e) {
                log.warn("MinIO 检查桶 {} 失败: {}", bucket.getName(), e.getMessage());
                return false;
            }
        }).toList();
    }

    /**
     * 更新桶信息（校验桶名唯一 & MinIO 存在）
     */
    @Transactional
    @Override
    public Bucket updateBucket(Bucket bucket) {
        Bucket existing = bucketRepository.findById(bucket.getId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        if (!existing.getName().equals(bucket.getName()) &&
                bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(existing.getName()).build()
            );
            if (!exists) {
                throw new ApiException(404, "MinIO 中不存在桶：" + existing.getName());
            }
        } catch (Exception e) {
            log.error("MinIO 校验桶失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        existing.setName(bucket.getName());
        existing.setDescription(bucket.getDescription());
        return bucketRepository.save(existing);
    }

    /**
     * 删除桶（从 MinIO 和数据库中都删除）
     */
    @Transactional
    @Override
    public void deleteBucket(Long id) {
        Bucket bucket = bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket.getName()).build()
            );
            if (exists) {
                log.info("尝试删除 MinIO 中桶：{}", bucket.getName());
                // 不建议直接删除 MinIO 桶，这里可根据你业务安全策略启用
                // minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket.getName()).build());
            } else {
                log.warn("MinIO 中桶不存在，不执行删除：{}", bucket.getName());
            }
        } catch (Exception e) {
            log.error("删除桶时 MinIO 校验失败", e);
            throw new ApiException(500, "MinIO 查询异常：" + e.getMessage());
        }

        bucketRepository.deleteById(id);
    }

    @Override
    public List<Bucket> getAccessibleBuckets(String uid) {
        // 获取用户拥有权限的桶ID列表
        List<Long> accessibleBucketIds = bucketPermissionService.getAccessibleBucketIds(uid);

        // 获取用户自己拥有的桶
        List<Bucket> ownBuckets = bucketRepository.findByOwnerUid(uid);

        // 查询所有桶，合并并去重
        List<Bucket> authorizedBuckets = bucketRepository.findAllById(accessibleBucketIds);

        // 合并ownBuckets和authorizedBuckets，去重
        Set<Bucket> resultSet = new HashSet<>();
        resultSet.addAll(ownBuckets);
        resultSet.addAll(authorizedBuckets);

        // 过滤存在于MinIO中的桶
        return resultSet.stream().filter(bucket -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket.getName()).build());
            } catch (Exception e) {
                log.warn("检查MinIO桶失败: {}", bucket.getName(), e);
                return false;
            }
        }).toList();
    }

}
