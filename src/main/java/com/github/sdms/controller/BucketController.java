package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.BucketPermissionDTO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.repository.*;
import com.github.sdms.service.BucketService;
import com.github.sdms.service.MinioService;
import com.github.sdms.util.AuthUtils;
import com.github.sdms.util.BucketUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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

    @Operation(summary = "创建存储桶", description = "管理员创建新的存储桶")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ResponseEntity<Bucket> createBucket(@RequestBody Bucket bucket) {
        if (bucket.getOwnerUid() == null || bucket.getOwnerUid().isEmpty()) {
            throw new ApiException("必须指定 ownerUid");
        }

        if (bucket.getLibraryCode() == null || bucket.getLibraryCode().isEmpty()) {
            // 从用户表查找该 uid 对应的用户，补充其 libraryCode
            User user = userRepository.findByUid(bucket.getOwnerUid())
                    .orElseThrow(() -> new ApiException(404, "ownerUid 无效，未找到对应用户"));
            bucket.setLibraryCode(user.getLibraryCode());
        }

        // 根据统一规则构造桶名
        String bucketName = BucketUtil.getBucketName(bucket.getOwnerUid(), bucket.getLibraryCode());
        bucket.setName(bucketName);

        // 创建存储桶
        Bucket createdBucket = bucketService.createBucket(bucket);

        // 创建对应的权限资源记录
        PermissionResource permissionResource = PermissionResource.builder()
                .name(bucket.getName()) // 例如，存储桶的名称
                .resourceKey(createdBucket.getId().toString()) // 存储桶的 ID 作为资源标识
                .resourceType("BUCKET") // 资源类型是 BUCKET
                .build();
        permissionResourceRepository.save(permissionResource); // 插入权限资源记录

        return ResponseEntity.ok(createdBucket);
    }

    @Operation(summary = "为用户分配存储桶权限", description = "管理员为用户分配存储桶权限")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/assign-bucket-permission")
    public ResponseEntity<String> assignBucketPermission(@RequestBody BucketPermissionDTO dto) {
        // 查找用户
        User user = userRepository.findByUid(dto.getUid())  // 使用 UID 查找用户
                .orElseThrow(() -> new ApiException(404, "User not found"));

        // 查找存储桶
        Bucket bucket = bucketRepository.findById(dto.getBucketId())
                .orElseThrow(() -> new ApiException(404, "Bucket not found"));

        // 检查是否已经有权限记录
        BucketPermission existingPermission = bucketPermissionRepository.findByUidAndBucketId(dto.getUid(), dto.getBucketId());

        if (existingPermission != null) {
            // 如果记录已存在，更新权限
            existingPermission.setPermission(dto.getPermission());
            existingPermission.setUpdatedAt(new Date());
            bucketPermissionRepository.save(existingPermission);
        } else {
            // 如果记录不存在，创建新的权限记录
            BucketPermission permission = BucketPermission.builder()
                    .uid(dto.getUid())  // 直接使用 uid 赋值
                    .bucketId(dto.getBucketId())  // 使用 bucketId
                    .permission(dto.getPermission())
                    .build();
            bucketPermissionRepository.save(permission);
        }

        // 获取对应的存储桶资源记录
        PermissionResource permissionResource = permissionResourceRepository.findByResourceKey(bucket.getId().toString())
                .orElseThrow(() -> new ApiException(404, "PermissionResource not found"));

        // 查找并更新角色权限记录
        RolePermission rolePermission = rolePermissionRepository.findByRoleTypeAndResource(user.getRoleType(), permissionResource)
                .orElse(null); // 如果没有记录，则为 null

        if (rolePermission != null) {
            // 如果记录存在，更新权限
            rolePermission.setPermission(PermissionType.fromString(dto.getPermission()));
            rolePermissionRepository.save(rolePermission);
        } else {
            // 如果记录不存在，创建新的角色权限记录
            RolePermission newRolePermission = RolePermission.builder()
                    .roleType(user.getRoleType())  // 使用用户的角色
                    .resource(permissionResource)  // 关联存储桶的资源
                    .permission(PermissionType.fromString(dto.getPermission()))  // 分配的权限
                    .build();
            rolePermissionRepository.save(newRolePermission);
        }

        return ResponseEntity.ok("Permission assigned or updated successfully");
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
