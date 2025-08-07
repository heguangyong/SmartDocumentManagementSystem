package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permission")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "系统权限、角色与访问控制接口")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取所有权限资源列表")
    @GetMapping("/resources")
    public List<PermissionResource> listResources() {
        return permissionService.listAllResources();
    }

    @Operation(summary = "获取某角色的权限列表")
    @GetMapping("/role/{role}")
    public List<RolePermission> getRolePermissions(@PathVariable String role) {
        return permissionService.getPermissionsByRole(role);
    }

    @Operation(summary = "为角色分配权限")
    @PostMapping("/role/{role}")
    public ApiResponse<Void> assignPermissions(@PathVariable String role, @RequestBody List<RolePermissionDTO> permissions) {
        permissionService.assignPermissions(role, permissions);
        return ApiResponse.success();
    }

    @Operation(summary = "移除角色的某个权限资源")
    @DeleteMapping("/role/{role}/resource/{resourceId}")
    public void removePermission(@PathVariable String role, @PathVariable Long resourceId) {
        permissionService.removePermission(role, resourceId);
    }

    @Operation(summary = "新增权限资源")
    @PostMapping("/resources")
    public ApiResponse<PermissionResource> addResource(@RequestBody PermissionResource permissionResource) {
        PermissionResource createdResource = permissionService.addResource(permissionResource);
        return ApiResponse.success(createdResource);
    }
}
