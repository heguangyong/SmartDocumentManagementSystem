package com.github.sdms.controller;

import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/resources")
    public List<PermissionResource> listResources() {
        return permissionService.listAllResources();
    }

    @GetMapping("/role/{role}")
    public List<RolePermission> getRolePermissions(@PathVariable String role) {
        return permissionService.getPermissionsByRole(role);
    }

    @PostMapping("/role/{role}")
    public void assignPermissions(@PathVariable String role, @RequestBody List<RolePermissionDTO> permissions) {
        permissionService.assignPermissions(role, permissions);
    }

    @DeleteMapping("/role/{role}/resource/{resourceId}")
    public void removePermission(@PathVariable String role, @PathVariable Long resourceId) {
        permissionService.removePermission(role, resourceId);
    }
}
