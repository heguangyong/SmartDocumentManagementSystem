package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.RoleType;
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
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
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
        return findUserOrThrow(uid).getRoleType() == RoleType.ADMIN;
    }

    @Override
    public boolean isLibrarian(String uid) {
        return findUserOrThrow(uid).getRoleType() == RoleType.LIBRARIAN;
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
        return userRepository.findByUid(uid)
                .map(AppUser::getLibraryCode)
                .orElseThrow(() -> new ApiException("无法根据 UID 找到用户：" + uid));
    }

    @Override
    public boolean hasWritePermission(String uid, String bucketId) {
        AppUser user = findUserOrThrow(uid);
        RoleType roleType = user.getRoleType();

        if (roleType == RoleType.ADMIN) return true;
        if (roleType == RoleType.LIBRARIAN) {
            return extractLibraryCode(bucketId).equalsIgnoreCase(user.getLibraryCode());
        }
        return isOwnBucket(uid, bucketId);
    }

}
