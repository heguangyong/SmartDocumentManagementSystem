package com.github.sdms.service.impl;

import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.repository.PermissionResourceRepository;
import com.github.sdms.repository.RolePermissionRepository;
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


    @Override
    public List<PermissionResource> listAllResources() {
        return resourceRepo.findAll();
    }

    @Override
    public List<RolePermission> getPermissionsByRole(String role) {
        return rolePermissionRepo.findByRole(role);
    }



    @Override
    public void removePermission(String role, Long resourceId) {
        rolePermissionRepo.deleteByRoleAndResource_Id(role, resourceId);
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

    /**
     * 为角色分配权限
     */
    @Override
    public void assignPermissions(String role, List<RolePermissionDTO> permissions) {
        // 校验角色是否存在
        if (!role.equals("LIBRARIAN") && !role.equals("ADMIN")) {
            throw new ApiException("无效角色");
        }

        // 遍历权限配置，进行资源与角色的关联
        for (RolePermissionDTO dto : permissions) {
            // 查找资源
            PermissionResource resource = permissionResourceRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ApiException("无效资源ID"));

            // 检查是否已有该权限
            if (rolePermissionRepository.existsByRoleAndResourceId(role, resource.getId())) {
                throw new ApiException("该角色已拥有此资源权限");
            }

            // 创建 RolePermission 关系
            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .resource(resource)
                    .permission(dto.getPermission())
                    .build();

            // 保存到数据库
            rolePermissionRepository.save(rp);
        }
    }
}
