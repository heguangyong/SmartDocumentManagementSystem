package com.github.sdms.service.impl;

import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.repository.PermissionResourceRepository;
import com.github.sdms.repository.RolePermissionRepository;
import com.github.sdms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionResourceRepository resourceRepo;
    private final RolePermissionRepository rolePermissionRepo;

    @Override
    public List<PermissionResource> listAllResources() {
        return resourceRepo.findAll();
    }

    @Override
    public List<RolePermission> getPermissionsByRole(String role) {
        return rolePermissionRepo.findByRole(role);
    }

    @Override
    @Transactional
    public void assignPermissions(String role, List<RolePermissionDTO> permissions) {
        for (RolePermissionDTO dto : permissions) {
            PermissionResource resource = resourceRepo.findById(dto.getResourceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid resource ID"));

            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .resource(resource)
                    .permission(dto.getPermission())
                    .build();

            rolePermissionRepo.save(rp);
        }
    }

    @Override
    public void removePermission(String role, Long resourceId) {
        rolePermissionRepo.deleteByRoleAndResource_Id(role, resourceId);
    }
}
