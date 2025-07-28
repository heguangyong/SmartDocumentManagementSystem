package com.github.sdms.service;

import com.github.sdms.model.Bucket;

import java.util.List;
import java.util.Set;

public interface BucketService {
    Bucket createBucket(Bucket bucket);

    Bucket getBucketById(Long id);

    List<Bucket> getAllBuckets();

    Bucket updateBucket(Bucket bucket);

    void deleteBucket(Long id);

    List<Bucket> getAccessibleBuckets(String uid);

    Bucket getUserDefaultBucket(String uid, String libraryCode);

    List<String> findBucketNamesByIds(Set<Long> allBucketIds);
}
