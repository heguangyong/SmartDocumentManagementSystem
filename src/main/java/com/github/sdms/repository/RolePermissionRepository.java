package com.github.sdms.repository;

import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    // 查询某个角色下的所有权限
    List<RolePermission> findByRoleType(RoleType roleType);

    // 删除某个角色对某资源的权限
    void deleteByRoleTypeAndResource_Id(RoleType roleType, Long resourceId);

    // 判断角色是否已有某资源的权限
    boolean existsByRoleTypeAndResourceId(RoleType roleType, Long resourceId);

    // 查询角色与资源对应的权限记录
    Optional<RolePermission> findByRoleTypeAndResource(RoleType roleType, PermissionResource resource);

    // ✅ 新增：判断角色对资源是否具有某权限
    boolean existsByRoleTypeAndResourceIdAndPermission(RoleType roleType, Long resourceId, PermissionType permission);

    // ✅ 新增：获取角色对资源的所有权限记录
    List<RolePermission> findByRoleTypeAndResourceId(RoleType roleType, Long resourceId);

    // ✅ 新增：获取某资源的所有权限记录
    List<RolePermission> findByResourceId(Long resourceId);

    // ✅ 新增：获取角色对所有资源的指定权限
    List<RolePermission> findByRoleTypeAndPermission(RoleType roleType, PermissionType permission);

    // ✅ 新增：删除角色对资源的某权限
    void deleteByRoleTypeAndResourceIdAndPermission(RoleType roleType, Long resourceId, PermissionType permission);

    // 使用 Spring Data JPA 语法，roleType 是枚举 RoleType，resource.resourceType 是字符串枚举 ResourceType
    List<RolePermission> findByRoleTypeInAndResource_ResourceType(List<RoleType> roleTypes, String resourceType);

    List<Long> findBucketResourceIdsByRoleType(RoleType roleType);

    void delete(Optional<RolePermission> rolePermission);
}
