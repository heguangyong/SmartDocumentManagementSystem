package com.github.sdms.repository;

import com.github.sdms.model.BucketPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BucketPermissionRepository extends JpaRepository<BucketPermission, Long> {

    boolean existsByUidAndBucketIdAndCanReadTrue(String uid, String bucketId);

    boolean existsByUidAndBucketIdAndCanWriteTrue(String uid, String bucketId);
}
