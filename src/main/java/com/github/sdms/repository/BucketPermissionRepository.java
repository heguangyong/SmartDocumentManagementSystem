package com.github.sdms.repository;

import com.github.sdms.model.BucketPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BucketPermissionRepository extends JpaRepository<BucketPermission, Long> {

    // 判断用户对某桶是否具备某权限（读/写等），使用LIKE模糊匹配权限字符串
    @Query("select case when count(bp) > 0 then true else false end from BucketPermission bp " +
            "where bp.userId = :userId and bp.bucketId = :bucketId and bp.permission like %:permission%")
    boolean hasPermission(@Param("userId") Long userId,
                          @Param("bucketId") Long bucketId,
                          @Param("permission") String permission);

    List<BucketPermission> findAllByUserId(Long userId);

    Optional<BucketPermission> findByUserIdAndBucketId(Long userId, Long bucketId);

    List<BucketPermission> findByUserId(Long userId);

    @Query("select bp.bucketId from BucketPermission bp where bp.userId = :userId")
    List<Long> findBucketIdsByUserId(Long userId);

    int countByBucketId(Long bucketId);

    List<BucketPermission> findByBucketId(Long bucketId);

    List<BucketPermission> findAllByBucketId(Long bucketId);

    Optional<BucketPermission> findByBucketIdAndUserId(Long bucketId, Long userId);

    void deleteByUserIdAndBucketId(Long userId, Long bucketId);

}
