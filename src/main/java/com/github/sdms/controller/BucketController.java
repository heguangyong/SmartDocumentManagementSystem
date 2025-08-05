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
import java.util.Optional;

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
    @Operation(summary = "列出某个桶的用户权限绑定列表")
    @GetMapping("/admin/{bucketId}/users")
    public ApiResponse<List<BucketUserPermissionDTO>> listBucketUserPermissions(@PathVariable Long bucketId) {

        // 1. 查桶是否存在
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        // 2. 获取权限绑定记录
        List<BucketPermission> permissions = bucketPermissionRepository.findByBucketId(bucketId);

        // 3. 构建返回列表
        List<BucketUserPermissionDTO> result = permissions.stream().map(bp -> {
            User user = userRepository.findById(bp.getUserId())
                    .orElse(null); // 用户可能已被删除

            BucketUserPermissionDTO dto = new BucketUserPermissionDTO();
            dto.setUserId(bp.getUserId());
            dto.setPermission(bp.getPermission());
            dto.setUpdatedAt(bp.getUpdatedAt());

            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setRoleType(user.getRoleType());
            }

            return dto;
        }).toList();

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

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "移除用户对桶的访问权限（使用 userId）")
    @PostMapping("/admin/remove-permission")
    public ApiResponse<Void> removeBucketPermissionByUserId(@RequestBody RemoveBucketPermissionRequest request) {

        // 1. 查找用户
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        // 2. 查找桶
        Bucket bucket = bucketRepository.findById(request.getBucketId())
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        // 3. 删除 BucketPermission（桶 + 用户ID）
        BucketPermission permission = bucketPermissionRepository.findByUserIdAndBucketId(user.getId(), bucket.getId());
        if (permission != null) {
            bucketPermissionRepository.delete(permission);
        }

        // 4. 删除 RolePermission（如果存在）
        PermissionResource resource = permissionResourceRepository.findByResourceKey(bucket.getId().toString())
                .orElse(null);

        if (resource != null) {
            Optional<RolePermission> rolePermission = rolePermissionRepository.findByRoleTypeAndResource(user.getRoleType(), resource);
            if (rolePermission.isPresent()) {
                rolePermissionRepository.delete(rolePermission);
            }
        }

        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新桶容量配置")
    @PutMapping("/admin/{bucketId}/capacity")
    public ApiResponse<Void> updateBucketCapacity(
            @PathVariable Long bucketId,
            @RequestBody UpdateBucketCapacityRequest request) {

        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new ApiException(404, "桶不存在"));

        bucket.setMaxCapacity(request.getMaxCapacity());
        bucket.setUpdatedAt(new Date());

        bucketRepository.save(bucket);

        return ApiResponse.success();
    }


    @Operation(summary = "管理员创建新的存储桶")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ApiResponse<Bucket> createBucket(@RequestBody CreateBucketRequest request) {

        if (request.getOwnerId() == null) {
            throw new ApiException("必须指定 ownerId");
        }

        User user = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ApiException(404, "ownerId 无效，未找到对应用户"));

        String libraryCode = user.getLibraryCode();

        // 构造桶名
        String bucketName = BucketUtil.getBucketName(request.getOwnerId(), libraryCode);

        Bucket bucket = Bucket.builder()
                .name(bucketName)
                .ownerId(user.getId())   // 为读者桶设置 UID
                .ownerId(user.getId())     // 支持馆员创建自定义桶
                .libraryCode(libraryCode)
                .description(request.getDescription())
                .maxCapacity(request.getMaxCapacity())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        Bucket createdBucket = bucketService.createBucket(bucket);

        // 创建权限资源
        PermissionResource permissionResource = PermissionResource.builder()
                .name(createdBucket.getName())
                .resourceKey(createdBucket.getId().toString())
                .resourceType("BUCKET")
                .build();
        permissionResourceRepository.save(permissionResource);

        return ApiResponse.success(createdBucket);
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
