package com.github.sdms.repository;

import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(String role);

    void deleteByRoleAndResource_Id(String role, Long resourceId);

    boolean existsByRoleAndResourceId(String role, Long id);

    Optional<RolePermission> findByRoleAndResource(Role role, PermissionResource permissionResource);
}