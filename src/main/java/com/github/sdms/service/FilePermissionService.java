package com.github.sdms.service;

import com.github.sdms.dto.FilePermissionAssignRequest;
import com.github.sdms.dto.FilePermissionDTO;
import com.github.sdms.dto.FilePermissionUpdateRequest;
import com.github.sdms.model.enums.PermissionType;

import java.util.List;

public interface FilePermissionService {
    List<FilePermissionDTO> getPermissionsByFileId(Long fileId);

    List<FilePermissionDTO> getPermissionsByUserId(Long userId);

    FilePermissionDTO assignPermission(FilePermissionAssignRequest request);

    FilePermissionDTO updatePermission(FilePermissionUpdateRequest request);

    void revokePermission(Long fileId, Long userId);

    boolean checkUserPermission(Long userId, Long fileId, PermissionType permissionType);
}

