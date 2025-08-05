package com.github.sdms.service;

import com.github.sdms.dto.BucketPageRequest;
import com.github.sdms.dto.BucketSummaryDTO;
import com.github.sdms.dto.BucketUserPermissionDTO;
import com.github.sdms.model.Bucket;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BucketService {
    Bucket createBucket(Bucket bucket);

    Bucket getBucketById(Long id);

    List<Bucket> getAllBuckets();

    Bucket updateBucket(Bucket bucket);

    void deleteBucket(Long id);

    List<Bucket> getAccessibleBuckets(Long userId);

    Bucket getUserDefaultBucket(Long userId, String libraryCode);

    List<String> findBucketNamesByIds(Set<Long> allBucketIds);

    Optional<Bucket> getOptionalBucketByName(String bucketName);

    Page<BucketSummaryDTO> pageBuckets(BucketPageRequest request);

    List<BucketUserPermissionDTO> getBucketUserPermissions(Long bucketId);
}
