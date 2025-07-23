package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.PermissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionValidatorImpl implements PermissionValidator {

    private final UserRepository userRepository;

    @Override
    public boolean canReadBucket(String uid, String bucketId) {
        AppUser user = findUserOrThrow(uid);
        Role role = user.getRole();

        if (role == Role.ADMIN) return true;
        if (role == Role.LIBRARIAN) {
            return extractLibraryCode(bucketId).equalsIgnoreCase(user.getLibraryCode());
        }
        return isOwnBucket(uid, bucketId);
    }

    @Override
    public boolean canWriteBucket(String uid, String bucketId) {
        return canReadBucket(uid, bucketId);
    }

    @Override
    public boolean canReadFile(String uid, Long fileId) {
        // TODO: 实现文件级别权限判断，如使用文件关联表
        return true;
    }

    @Override
    public boolean canWriteFile(String uid, Long fileId) {
        // TODO: 实现文件级别权限判断，如使用文件关联表
        return true;
    }

    @Override
    public boolean isAdmin(String uid) {
        return findUserOrThrow(uid).getRole() == Role.ADMIN;
    }

    @Override
    public boolean isLibrarian(String uid) {
        return findUserOrThrow(uid).getRole() == Role.LIBRARIAN;
    }

    private AppUser findUserOrThrow(String uid) {
        return userRepository.findByUidAndLibraryCode(uid, extractLibraryCodeFromUid(uid))
                .orElseThrow(() -> new ApiException("找不到用户: " + uid));
    }

    private boolean isOwnBucket(String uid, String bucketId) {
        return bucketId.toLowerCase().contains(uid.toLowerCase());
    }

    private String extractLibraryCode(String bucketId) {
        String[] parts = bucketId.split("-");
        if (parts.length >= 3) return parts[2];
        throw new ApiException("提取桶名中的LibraryCode失败: " + bucketId);
    }

    private String extractLibraryCodeFromUid(String uid) {
        // TODO: 从 uid 中提取 libraryCode，当前为默认实现
        return "defaultlib";
    }
}
