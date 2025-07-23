package com.github.sdms.repository;

import com.github.sdms.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(String role);
    void deleteByRoleAndResource_Id(String role, Long resourceId);
}