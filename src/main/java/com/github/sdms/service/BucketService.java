package com.github.sdms.service;

import com.github.sdms.model.Bucket;

import java.util.List;

public interface BucketService {
    Bucket createBucket(Bucket bucket);

    Bucket getBucketById(Long id);

    List<Bucket> getAllBuckets();

    Bucket updateBucket(Bucket bucket);

    void deleteBucket(Long id);

}
