package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Bucket;
import com.github.sdms.service.BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bucket")
@RequiredArgsConstructor
public class BucketController {

    private final BucketService bucketService;

    /**
     * 创建桶
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ApiResponse<Bucket> createBucket(@RequestBody Bucket bucket) {
        Bucket created = bucketService.createBucket(bucket);
        return ApiResponse.success(created);
    }

    /**
     * 根据 ID 获取桶信息
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<Bucket> getBucketById(@PathVariable Long id) {
        Bucket bucket = bucketService.getBucketById(id);
        return ApiResponse.success(bucket);
    }

    /**
     * 获取所有桶
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ApiResponse<List<Bucket>> listBuckets() {
        List<Bucket> buckets = bucketService.getAllBuckets();
        return ApiResponse.success(buckets);
    }

    /**
     * 更新桶
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update/{id}")
    public ApiResponse<Bucket> updateBucket(@PathVariable Long id, @RequestBody Bucket bucket) {
        bucket.setId(id);
        Bucket updated = bucketService.updateBucket(bucket);
        return ApiResponse.success(updated);
    }

    /**
     * 删除桶
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteBucket(@PathVariable Long id) {
        bucketService.deleteBucket(id);
        return ApiResponse.success();
    }
}
