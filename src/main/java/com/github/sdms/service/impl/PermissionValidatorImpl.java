package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.FilePermissionService;
import com.github.sdms.service.PermissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionValidatorImpl implements PermissionValidator {

    private final UserRepository userRepository;
    private final FilePermissionService filePermissionService;




    @Override
    public boolean canReadBucket(Long userId, String bucketId) {
        User user = findUserOrThrow(userId);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            return extractLibraryCode(bucketId).equalsIgnoreCase(user.getLibraryCode());
        }
        return isOwnBucket(userId, bucketId);
    }

    @Override
    public boolean canWriteBucket(Long userId, String bucketId) {
        return canReadBucket(userId, bucketId);
    }

    @Override
    public boolean canReadFile(Long userId, Long fileId) {
        return filePermissionService.checkUserPermission(userId, fileId, PermissionType.READ);
    }

    @Override
    public boolean canWriteFile(Long userId, Long fileId) {
        return filePermissionService.checkUserPermission(userId, fileId, PermissionType.WRITE);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return findUserOrThrow(userId).getRoleType() == RoleType.ADMIN;
    }

    @Override
    public boolean isLibrarian(Long userId) {
        return findUserOrThrow(userId).getRoleType() == RoleType.LIBRARIAN;
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("找不到用户 ID: " + userId));
    }

    private boolean isOwnBucket(Long userId, String bucketId) {
        return bucketId.toLowerCase().contains(userId.toString());
    }

    private String extractLibraryCode(String bucketId) {
        String[] parts = bucketId.split("-");
        if (parts.length >= 3) return parts[2];
        throw new ApiException("提取桶名中的LibraryCode失败: " + bucketId);
    }

    @Override
    public boolean hasWritePermission(Long userId, String bucketId) {
        User user = findUserOrThrow(userId);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            return extractLibraryCode(bucketId).equalsIgnoreCase(user.getLibraryCode());
        }
        return isOwnBucket(userId, bucketId);
    }
}
