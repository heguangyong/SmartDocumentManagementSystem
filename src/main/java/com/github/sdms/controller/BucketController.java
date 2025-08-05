package com.github.sdms.controller;

import com.github.sdms.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Objects;

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
        Page<BucketSummaryDTO> result = bucketService.pageBuckets(request);
        return ApiResponse.success(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询指定桶的用户权限列表")
    @GetMapping("/admin/{bucketId}/permissions")
    public ApiResponse<List<BucketUserPermissionDTO>> listBucketUserPermissions(@PathVariable Long bucketId) {
        // 查找桶是否存在
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        // 查找该桶所有权限配置记录
        List<BucketPermission> permissionList = bucketPermissionRepository.findAllByBucketId(bucketId);

        List<BucketUserPermissionDTO> result = permissionList.stream().map(permission -> {
            // 根据 uid 查用户（REQUIRED: uid → user）
            User user = userRepository.findById(permission.getUserId())
                    .orElse(null); // 如果找不到用户也不会终止

            if (user != null) {
                return BucketUserPermissionDTO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .roleType(user.getRoleType().name())
                        .permission(permission.getPermission())
                        .build();
            } else {
                // 如果 user 查询不到，说明是系统异常数据
                return null;
            }
        }).filter(Objects::nonNull).toList();

        return ApiResponse.success(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "为桶分配用户权限（使用 userId）")
    @PostMapping("/admin/assign-permission")
    public ApiResponse<Void> assignBucketPermissionByUserId(@RequestBody AssignBucketPermissionRequest request) {

        // 1. 查找用户
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        // 2. 查找桶
        Bucket bucket = bucketRepository.findById(request.getBucketId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        // 3. 查找并处理 BucketPermission（bucketId + uid）
        Long userId = user.getId(); // 若该用户无 uid（本地账户），该字段可为空
        if (userId == null) {
            throw new ApiException("该用户不支持分配桶权限（无 UID）");
        }

        BucketPermission existing = bucketPermissionRepository.findByUserIdAndBucketId(userId, request.getBucketId());

        if (existing != null) {
            // 更新已有权限
            existing.setPermission(request.getPermission());
            existing.setUpdatedAt(new Date());
            bucketPermissionRepository.save(existing);
        } else {
            // 新建权限记录
            BucketPermission newPermission = BucketPermission.builder()
                    .userId(userId)
                    .bucketId(request.getBucketId())
                    .permission(request.getPermission())
                    .build();
            bucketPermissionRepository.save(newPermission);
        }

        // 4. 同步权限资源 → RolePermission（可选处理）
        PermissionResource permissionResource = permissionResourceRepository
                .findByResourceKey(bucket.getId().toString())
                .orElseThrow(() -> new ApiException(404, "桶权限资源未注册"));

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleTypeAndResource(user.getRoleType(), permissionResource)
                .orElse(null);

        if (rolePermission != null) {
            rolePermission.setPermission(PermissionType.fromString(request.getPermission()));
            rolePermissionRepository.save(rolePermission);
        } else {
            RolePermission newRolePermission = RolePermission.builder()
                    .roleType(user.getRoleType())
                    .resource(permissionResource)
                    .permission(PermissionType.fromString(request.getPermission()))
                    .build();
            rolePermissionRepository.save(newRolePermission);
        }

        return ApiResponse.success();
    }


    @Operation(summary = "创建存储桶", description = "管理员创建新的存储桶")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ResponseEntity<Bucket> createBucket(@RequestBody Bucket bucket) {
        if (bucket.getOwnerId() == null) {
            throw new ApiException("必须指定 ownerId");
        }

        if (bucket.getLibraryCode() == null || bucket.getLibraryCode().isEmpty()) {
            // 从用户表查找该 ownerId 对应的用户，补充其 libraryCode
            User user = userRepository.findById(bucket.getOwnerId())
                    .orElseThrow(() -> new ApiException(404, "ownerId 无效，未找到对应用户"));
            bucket.setLibraryCode(user.getLibraryCode());
        }

        // 根据统一规则构造桶名
        String bucketName = BucketUtil.getBucketName(bucket.getOwnerId(), bucket.getLibraryCode());
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
        User user = userRepository.findById(dto.getUserId())  // 使用 UID 查找用户
                .orElseThrow(() -> new ApiException(404, "User not found"));

        // 查找存储桶
        Bucket bucket = bucketRepository.findById(dto.getBucketId())
                .orElseThrow(() -> new ApiException(404, "Bucket not found"));

        // 检查是否已经有权限记录
        BucketPermission existingPermission = bucketPermissionRepository.findByUserIdAndBucketId(dto.getUserId(), dto.getBucketId());

        if (existingPermission != null) {
            // 如果记录已存在，更新权限
            existingPermission.setPermission(dto.getPermission());
            existingPermission.setUpdatedAt(new Date());
            bucketPermissionRepository.save(existingPermission);
        } else {
            // 如果记录不存在，创建新的权限记录
            BucketPermission permission = BucketPermission.builder()
                    .userId(dto.getUserId())  // 直接使用 uid 赋值
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
        Long userId = AuthUtils.getCurrentUserId();
        List<Bucket> buckets = bucketService.getAccessibleBuckets(userId);
        return ApiResponse.success(buckets);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "用户创建自己的桶")
    @PostMapping("/user/create")
    public ApiResponse<Bucket> createBucketByUser(@RequestBody Bucket bucket) {
        bucket.setOwnerId(AuthUtils.getCurrentUserId());
        Bucket created = bucketService.createBucket(bucket);
        return ApiResponse.success(created);
    }
}
