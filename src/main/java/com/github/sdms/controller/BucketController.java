package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Bucket;
import com.github.sdms.service.BucketService;
import com.github.sdms.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/bucket")
@RequiredArgsConstructor
public class BucketController {

    private final BucketService bucketService;

    // ========================= 管理员操作 =========================

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员创建新的桶")
    @PostMapping("/admin/create")
    public ApiResponse<Bucket> createBucketByAdmin(@RequestBody Bucket bucket) {
        Bucket created = bucketService.createBucket(bucket);
        return ApiResponse.success(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "根据ID获取桶的详细信息")
    @GetMapping("/admin/{id}")
    public ApiResponse<Bucket> getBucketById(@PathVariable Long id) {
        Bucket bucket = bucketService.getBucketById(id);
        return ApiResponse.success(bucket);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有桶的列表")
    @GetMapping("/admin/list")
    public ApiResponse<List<Bucket>> listBuckets() {
        List<Bucket> buckets = bucketService.getAllBuckets();
        return ApiResponse.success(buckets);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员更新桶的详细信息")
    @PutMapping("/admin/update/{id}")
    public ApiResponse<Bucket> updateBucket(@PathVariable Long id, @RequestBody Bucket bucket) {
        bucket.setId(id);
        Bucket updated = bucketService.updateBucket(bucket);
        return ApiResponse.success(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员删除指定ID的桶")
    @DeleteMapping("/admin/delete/{id}")
    public ApiResponse<Void> deleteBucket(@PathVariable Long id) {
        bucketService.deleteBucket(id);
        return ApiResponse.success();
    }

    // ========================= 用户操作 =========================

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户所有有权限访问的桶（包括自己拥有的和被授权的）")
    @GetMapping("/user/list")
    public ApiResponse<List<Bucket>> listUserBuckets() {
        String uid = AuthUtils.getUid();
        List<Bucket> buckets = bucketService.getAccessibleBuckets(uid);
        return ApiResponse.success(buckets);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "用户创建自己的桶")
    @PostMapping("/user/create")
    public ApiResponse<Bucket> createBucketByUser(@RequestBody Bucket bucket) {
        bucket.setOwnerUid(AuthUtils.getUid());
        Bucket created = bucketService.createBucket(bucket);
        return ApiResponse.success(created);
    }
}
