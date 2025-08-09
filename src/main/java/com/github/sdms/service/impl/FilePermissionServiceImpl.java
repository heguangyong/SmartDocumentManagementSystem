package com.github.sdms.service.impl;

import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.*;
import com.github.sdms.service.FilePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FilePermissionServiceImpl implements FilePermissionService {

    private final FilePermissionRepository filePermissionRepository;
    private final UserRepository userRepository;
    private final UserFileRepository userFileRepository;
    private final BucketPermissionRepository bucketPermissionRepository;
    private final BucketRepository bucketRepository;

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

    /**
     * 新增：获取用户对指定文件的有效权限集合
     */
    @Override
    public Set<PermissionType> getEffectiveFilePermissions(Long userId, Long fileId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + userId));
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("找不到文件 ID: " + fileId));

        // 管理员拥有所有权限
        if (user.getRoleType() == RoleType.ADMIN) {
            return EnumSet.allOf(PermissionType.class);
        }

        // 文件自定义权限
        FilePermission filePerm = filePermissionRepository.findByUserAndFile(user, file);
        if (filePerm != null) {
            return parsePermissionString(filePerm.getPermission());
        }

        // 继承桶权限
        Bucket bucket = bucketRepository.findByName(file.getBucket())
                .orElseThrow(() -> new ApiException("桶不存在"));
        Optional<BucketPermission> bucketPermOpt = bucketPermissionRepository.findByBucketIdAndUserId(bucket.getId(), userId);
        return bucketPermOpt.map(bp -> parsePermissionString(bp.getPermission()))
                .orElse(Collections.emptySet());
    }

    /**
     * 新增：判断用户对文件是否拥有某个权限
     */
    @Override
    public boolean hasPermission(Long userId, Long fileId, PermissionType requiredPermission) {
        return getEffectiveFilePermissions(userId, fileId).contains(requiredPermission);
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
        dto.setPermission(PermissionType.fromString(permission.getPermission()));
        return dto;
    }

    @Override
    public FileSharePermissionDTO getFileSharePermission(Long fileId, Long targetUserId) {
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("文件不存在"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException("目标用户不存在"));

        FileSharePermissionDTO dto = new FileSharePermissionDTO();
        dto.setFileId(fileId);
        dto.setTargetUserId(targetUserId);
        dto.setTargetUsername(targetUser.getUsername());

        FilePermission filePerm = filePermissionRepository.findByUserAndFile(targetUser, file);
        if (filePerm != null) {
            dto.setInherited(false);
            dto.setPermissions(parsePermissionString(filePerm.getPermission()));
        } else {
            Bucket bucket = bucketRepository.findByName(file.getBucket())
                    .orElseThrow(() -> new ApiException("桶不存在"));

            Optional<BucketPermission> bucketPermOpt = bucketPermissionRepository.findByBucketIdAndUserId(bucket.getId(), targetUserId);
            if (bucketPermOpt.isPresent()) {
                dto.setInherited(true);
                dto.setPermissions(parsePermissionString(bucketPermOpt.get().getPermission()));
            } else {
                dto.setInherited(true);
                dto.setPermissions(Collections.emptySet());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public FileSharePermissionDTO assignFileSharePermission(FileSharePermissionAssignRequest request) {
        UserFile file = userFileRepository.findById(request.getFileId())
                .orElseThrow(() -> new ApiException("文件不存在"));
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ApiException("目标用户不存在"));

        if (request.isInherit()) {
            FilePermission existing = filePermissionRepository.findByUserAndFile(targetUser, file);
            if (existing != null) {
                filePermissionRepository.delete(existing);
            }
        } else {
            String permStr = request.getPermissions().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            FilePermission permission = filePermissionRepository.findByUserAndFile(targetUser, file);
            if (permission == null) {
                permission = FilePermission.builder()
                        .user(targetUser)
                        .file(file)
                        .build();
            }
            permission.setPermission(permStr);
            filePermissionRepository.save(permission);
        }
        return getFileSharePermission(request.getFileId(), request.getTargetUserId());
    }

    private Set<PermissionType> parsePermissionString(String permissionStr) {
        if (permissionStr == null || permissionStr.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(permissionStr.split(","))
                .map(String::trim)
                .map(PermissionType::valueOf)
                .collect(Collectors.toSet());
    }
}
