package com.github.sdms.service.impl;

import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.ResourceType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.*;
import com.github.sdms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionResourceRepository resourceRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final PermissionResourceRepository permissionResourceRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final BucketRepository bucketRepository;
    private final UserPermissionRepository userPermissionRepository;


    @Override
    public List<PermissionResource> listAllResources() {
        return resourceRepo.findAll();
    }

    @Override
    public List<RolePermission> getPermissionsByRole(String role) {
        return rolePermissionRepo.findByRoleType(RoleType.fromString(role));
    }



    @Override
    public void removePermission(String role, Long resourceId) {
        rolePermissionRepo.deleteByRoleTypeAndResource_Id(RoleType.fromString(role), resourceId);
    }

    /**
     * 插入新资源
     */
    @Override
    public PermissionResource addResource(PermissionResource permissionResource) {
        // 可以做额外的校验，例如资源是否已经存在
        if (permissionResourceRepository.existsByResourceKey(permissionResource.getResourceKey())) {
            throw new ApiException("资源已存在");
        }
        return permissionResourceRepository.save(permissionResource);
    }

    @Override
    public void addBucketPermission(Long userId, String bucketName, PermissionType type) {
        // 1. 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        // 2. 获取桶
        Bucket bucket = bucketRepository.findByName(bucketName)
                .orElseThrow(() -> new ApiException(404, "指定的桶不存在: " + bucketName));

        // 3. 查找或创建资源
        PermissionResource resource = permissionResourceRepository
                .findByResourceTypeAndResourceKey(ResourceType.BUCKET.name(), String.valueOf(bucket.getId()))
                .orElseGet(() -> {
                    PermissionResource newResource = PermissionResource.builder()
                            .resourceType(ResourceType.BUCKET.name())
                            .resourceKey(String.valueOf(bucket.getId()))
                            .name(bucket.getName())
                            .build();
                    return permissionResourceRepository.save(newResource);
                });

        // 4. 检查用户是否已有该资源的权限
        boolean alreadyHasPermission = userPermissionRepository.existsByUserIdAndPermissionTypeAndResourceId(
                userId, type.name(), resource.getId());

        if (!alreadyHasPermission) {
            UserPermission permission = UserPermission.builder()
                    .userId(userId)
                    .permissionType(type.name())
                    .resourceId(resource.getId())
                    .build();
            userPermissionRepository.save(permission);
        }
    }


    /**
     * 为角色分配权限
     */
    @Override
    public void assignPermissions(String role, List<RolePermissionDTO> permissions) {
        // 校验角色是否存在
        RoleType roleTypeEnum = RoleType.fromString(role);


        // 遍历权限配置，进行资源与角色的关联
        for (RolePermissionDTO dto : permissions) {
            // 查找资源
            PermissionResource resource = permissionResourceRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ApiException("无效资源ID"));

            // 检查是否已有该权限
            if (rolePermissionRepository.existsByRoleTypeAndResourceId(roleTypeEnum, resource.getId())) {
                throw new ApiException("该角色已拥有此资源权限");
            }

            // 创建 RolePermission 关系
            RolePermission rp = RolePermission.builder()
                    .roleType(roleTypeEnum)
                    .resource(resource)
                    .permission(PermissionType.fromString(dto.getPermission()))
                    .build();

            // 保存到数据库
            rolePermissionRepository.save(rp);
        }
    }
}
