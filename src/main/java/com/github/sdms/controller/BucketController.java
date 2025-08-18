package com.github.sdms.controller;

import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.User;
import com.github.sdms.repository.*;
import com.github.sdms.service.BucketService;
import com.github.sdms.service.MinioService;
import com.github.sdms.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bucket")
@RequiredArgsConstructor
public class BucketController {

    private final BucketService bucketService;
    private final BucketRepository bucketRepository;
    private final PermissionResourceRepository permissionResourceRepository;
    private final BucketPermissionRepository bucketPermissionRepository;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MinioService minioService;

    // ========================= 管理员操作 =========================

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询所有存储桶（含容量、人数等）")
    @PostMapping("/admin/page")
    public ApiResponse<Page<BucketSummaryDTO>> pageBuckets(@RequestBody BucketPageRequest request) {
        return ApiResponse.success(bucketService.pageBuckets(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "列出某个桶的用户权限（含来源）")
    @GetMapping("/admin/{bucketId}/users")
    public ApiResponse<List<BucketUserPermissionDTO>> listBucketUserPermissions(@PathVariable Long bucketId) {
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
        return ApiResponse.success(bucketService.getBucketUserPermissionsWithSource(bucketId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量为桶分配用户权限")
    @PostMapping("/admin/assign-permissions")
    public ApiResponse<Void> assignBucketPermissions(@RequestBody BatchAssignBucketPermissionRequest request) {
        Bucket bucket = bucketRepository.findById(request.getBucketId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
        bucketService.batchAssignPermissions(request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量为桶分配用户权限2")
    @PostMapping("/admin/assign-permissions2")
    public ApiResponse<Void> assignBucketPermissions2(@RequestBody BucketUserPermissionsRequest request) {

        Long bucketId = request.getBucketId();

        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        bucketService.batchAssignPermissions2(bucketId, request.getUserPermissions());

        return ApiResponse.success();
    }



    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "移除用户对桶的访问权限")
    @PostMapping("/admin/remove-permission")
    public ApiResponse<Void> removeBucketPermission(@RequestBody RemoveBucketPermissionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        Bucket bucket = bucketRepository.findById(request.getBucketId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));
        bucketService.removeBucketPermission(user.getId(),bucket.getId());
        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新桶容量配置")
    @PutMapping("/admin/{bucketId}/capacity")
    public ApiResponse<Void> updateBucketCapacity(@PathVariable Long bucketId,
                                                  @RequestBody UpdateBucketCapacityRequest request) {
        bucketService.updateBucketCapacity(bucketId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "管理员创建新的存储桶")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ApiResponse<Bucket> createBucket(@RequestBody CreateBucketRequest request) {
        if (request.getOwnerId() == null) {
            Long userId = JwtUtil.getCurrentUserIdOrThrow();
            request.setOwnerId(userId);
        }
        return ApiResponse.success(bucketService.createBucketByAdmin(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "根据ID获取桶的详细信息")
    @GetMapping("/admin/{id}")
    public ApiResponse<Bucket> getBucketById(@PathVariable Long id) {
        return ApiResponse.success(bucketService.getBucketById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有桶的列表")
    @GetMapping("/admin/list")
    public ApiResponse<List<Bucket>> listBuckets() {
        return ApiResponse.success(bucketService.getAllBuckets());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员更新桶的详细信息")
    @PutMapping("/admin/update")
    public ApiResponse<Bucket> updateBucket(
            @RequestBody UpdateBucketRequest request) {
        return ApiResponse.success(bucketService.updateBucketInfo(request));
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
    @Operation(summary = "获取当前用户所有有权限访问的桶")
    @GetMapping("/user/list")
    public ApiResponse<List<Bucket>> listUserBuckets() {
        Long userId = JwtUtil.getCurrentUserIdOrThrow();
        return ApiResponse.success(bucketService.getAccessibleBuckets(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户在某桶的最终权限")
    @GetMapping("/user/{bucketId}/effective-permission")
    public ApiResponse<List<String>> getEffectiveBucketPermission(@PathVariable Long bucketId) {
        Long userId = JwtUtil.getCurrentUserIdOrThrow();
        return ApiResponse.success(bucketService.getEffectiveBucketPermission(userId, bucketId));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "用户创建自己的桶")
    @PostMapping("/user/create")
    public ApiResponse<Bucket> createBucketByUser(@RequestBody Bucket bucket) {
        bucket.setOwnerId(JwtUtil.getCurrentUserIdOrThrow());
        return ApiResponse.success(bucketService.createBucket(bucket));
    }
}
