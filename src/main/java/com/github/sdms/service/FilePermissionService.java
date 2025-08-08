package com.github.sdms.service;

import com.github.sdms.dto.*;
import com.github.sdms.model.enums.PermissionType;

import java.util.List;

public interface FilePermissionService {
    List<FilePermissionDTO> getPermissionsByFileId(Long fileId);

    List<FilePermissionDTO> getPermissionsByUserId(Long userId);

    FilePermissionDTO assignPermission(FilePermissionAssignRequest request);

    FilePermissionDTO updatePermission(FilePermissionUpdateRequest request);

    void revokePermission(Long fileId, Long userId);

    boolean checkUserPermission(Long userId, Long fileId, PermissionType permissionType);

    /**
     * 查询文件针对目标用户的权限详情（含是否继承自桶权限）
     */
    FileSharePermissionDTO getFileSharePermission(Long fileId, Long targetUserId);

    /**
     * 分配文件权限给目标用户，支持继承桶权限或自定义权限
     */
    FileSharePermissionDTO assignFileSharePermission(FileSharePermissionAssignRequest request);
}

