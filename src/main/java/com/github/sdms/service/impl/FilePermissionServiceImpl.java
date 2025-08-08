package com.github.sdms.service.impl;

import com.github.sdms.dto.FilePermissionAssignRequest;
import com.github.sdms.dto.FilePermissionDTO;
import com.github.sdms.dto.FilePermissionUpdateRequest;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.FilePermission;
import com.github.sdms.model.User;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.FilePermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.FilePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FilePermissionServiceImpl implements FilePermissionService {

    private final FilePermissionRepository filePermissionRepository;
    private final UserRepository userRepository;
    private final UserFileRepository userFileRepository;

    @Override
    public List<FilePermissionDTO> getPermissionsByFileId(Long fileId) {
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("找不到文件 ID: " + fileId));
        return filePermissionRepository.findByFile(file).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FilePermissionDTO> getPermissionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + userId));
        return filePermissionRepository.findByUser(user).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FilePermissionDTO assignPermission(FilePermissionAssignRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + request.getUserId()));
        UserFile file = userFileRepository.findById(request.getFileId())
                .orElseThrow(() -> new ApiException("找不到文件 ID: " + request.getFileId()));

        FilePermission permission = filePermissionRepository.findByUserAndFile(user, file);
        if (permission == null) {
            permission = FilePermission.builder()
                    .user(user)
                    .file(file)
                    .permission(request.getPermission().name())
                    .build();
        } else {
            permission.setPermission(request.getPermission().name());
        }
        filePermissionRepository.save(permission);
        return toDTO(permission);
    }

    @Override
    public FilePermissionDTO updatePermission(FilePermissionUpdateRequest request) {
        FilePermission permission = filePermissionRepository.findById(request.getId())
                .orElseThrow(() -> new ApiException("找不到权限记录 ID: " + request.getId()));
        permission.setPermission(request.getPermission().name());
        filePermissionRepository.save(permission);
        return toDTO(permission);
    }

    @Override
    public void revokePermission(Long fileId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + userId));
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("找不到文件 ID: " + fileId));
        FilePermission permission = filePermissionRepository.findByUserAndFile(user, file);
        if (permission != null) {
            filePermissionRepository.delete(permission);
        }
    }

    /**
     * 核心权限校验
     * 管理员角色自动拥有所有权限
     * 其余用户则基于文件权限记录校验
     */
    @Override
    public boolean checkUserPermission(Long userId, Long fileId, PermissionType permissionType) {
        User user = userRepository.findById(userId).orElse(null);
        UserFile file = userFileRepository.findById(fileId).orElse(null);
        if (user == null || file == null) return false;

        if (user.getRoleType() == RoleType.ADMIN) return true;

        FilePermission permission = filePermissionRepository.findByUserAndFile(user, file);
        if (permission == null) return false;

        PermissionType assigned = PermissionType.fromString(permission.getPermission());
        return permissionIncludes(assigned, permissionType);
    }


    private boolean permissionIncludes(PermissionType assigned, PermissionType required) {
        if (assigned == PermissionType.DELETE) return true;
        if (assigned == PermissionType.WRITE) return required == PermissionType.READ || required == PermissionType.WRITE;
        return assigned == PermissionType.READ && required == PermissionType.READ;
    }

    private FilePermissionDTO toDTO(FilePermission permission) {
        FilePermissionDTO dto = new FilePermissionDTO();
        dto.setId(permission.getId());
        dto.setUserId(permission.getUser().getId());
        dto.setFileId(permission.getFile().getId());
        // 安全转换字符串到枚举
        dto.setPermission(PermissionType.fromString(permission.getPermission()));
        return dto;
    }

}

