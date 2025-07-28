package com.github.sdms.repository;

import com.github.sdms.model.BucketPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BucketPermissionRepository extends JpaRepository<BucketPermission, Long> {

    // 判断用户对某桶是否具备某权限（读/写等），使用LIKE模糊匹配权限字符串
    @Query("select case when count(bp) > 0 then true else false end from BucketPermission bp " +
            "where bp.uid = :uid and bp.bucketId = :bucketId and bp.permission like %:permission%")
    boolean hasPermission(@Param("uid") String uid,
                          @Param("bucketId") Long bucketId,
                          @Param("permission") String permission);

    List<BucketPermission> findAllByUid(String uid);

    BucketPermission findByUidAndBucketId(String uid, Long bucketId);

    List<BucketPermission> findByUid(String uid);

    List<Long> findBucketIdsByUid(String uid);
}
