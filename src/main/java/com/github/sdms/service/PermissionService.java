package com.github.sdms.service;

import com.github.sdms.dto.RolePermissionDTO;
import com.github.sdms.model.PermissionResource;
import com.github.sdms.model.RolePermission;
import com.github.sdms.model.enums.PermissionType;

import java.util.List;

public interface PermissionService {
    List<PermissionResource> listAllResources();

    List<RolePermission> getPermissionsByRole(String role);

    void assignPermissions(String role, List<RolePermissionDTO> permissions);

    void removePermission(String role, Long resourceId);

    PermissionResource addResource(PermissionResource permissionResource);

    void addBucketPermission(String uid, String bucketName, PermissionType type);

}

