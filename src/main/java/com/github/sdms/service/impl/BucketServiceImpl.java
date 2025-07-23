package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.repository.BucketRepository;
import com.github.sdms.service.BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BucketServiceImpl implements BucketService {

    private final BucketRepository bucketRepository;

    @Transactional
    @Override
    public Bucket createBucket(Bucket bucket) {
        if (bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }
        return bucketRepository.save(bucket);
    }

    @Override
    public Bucket getBucketById(Long id) {
        return bucketRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
    }

    @Override
    public List<Bucket> getAllBuckets() {
        return bucketRepository.findAll();
    }

    @Transactional
    @Override
    public Bucket updateBucket(Bucket bucket) {
        Bucket existing = bucketRepository.findById(bucket.getId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        // 检查重名但允许相同 ID
        if (!existing.getName().equals(bucket.getName()) &&
                bucketRepository.existsByName(bucket.getName())) {
            throw new ApiException(400, "桶名已存在");
        }

        existing.setName(bucket.getName());
        existing.setDescription(bucket.getDescription());
        return bucketRepository.save(existing);
    }

    @Transactional
    @Override
    public void deleteBucket(Long id) {
        if (!bucketRepository.existsById(id)) {
            throw new ApiException(404, "桶不存在");
        }
        bucketRepository.deleteById(id);
    }
}
