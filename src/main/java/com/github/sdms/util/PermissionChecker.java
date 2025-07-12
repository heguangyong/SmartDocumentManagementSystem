package com.github.sdms.util;

import com.github.sdms.model.Folder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final JwtUtil jwtUtil;

    /**
     * 检查当前用户是否可以访问指定 UID 的资源
     * 支持多角色和多租户逻辑：
     * - READER：只能访问自己的资源（uid一致，且所属馆一致）
     * - LIBRARIAN：只能访问本馆用户资源（libraryCode一致）
     * - ADMIN：可访问所有资源
     */
    public void checkAccess(String targetUid, String targetLibraryCode) {
        String currentUid = jwtUtil.getCurrentUsername();
        String currentRole = jwtUtil.getCurrentRole();
        String currentLibraryCode = jwtUtil.getCurrentLibraryCode();

        if (currentUid == null || currentRole == null) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }

        if ("ADMIN".equals(currentRole)) {
            // admin 可访问所有资源
            return;
        }

        if ("LIBRARIAN".equals(currentRole)) {
            // librarian 可访问同一个馆下所有用户资源
            if (!currentLibraryCode.equals(targetLibraryCode)) {
                throw new RuntimeException("越权访问：不能访问其他馆点数据");
            }
            return;
        }

        if ("READER".equals(currentRole)) {
            // reader 只能访问自己
            if (!currentUid.equals(targetUid) || !currentLibraryCode.equals(targetLibraryCode)) {
                throw new RuntimeException("越权访问：无权访问其他用户或其他馆点资源");
            }
            return;
        }

        throw new RuntimeException("未识别的角色：" + currentRole);
    }

    /**
     * 原 checkAccess 方法（兼容旧逻辑，默认仅本人和 admin 可访问）
     */
    public void checkAccess(String targetUid) {
        String currentUid = jwtUtil.getCurrentUsername();
        if (currentUid == null) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }

        if (!targetUid.equals(currentUid) && !jwtUtil.isAdmin()) {
            throw new RuntimeException("越权访问：无权访问其他用户资源");
        }
    }

    /**
     * 检查是否为管理员
     */
    public void requireAdmin() {
        if (!jwtUtil.isAdmin()) {
            throw new RuntimeException("只有管理员才能执行该操作");
        }
    }

    /**
     * 检查是否为指定角色
     */
    public void requireRole(String requiredRole) {
        String currentRole = jwtUtil.getCurrentRole();
        if (!requiredRole.equals(currentRole)) {
            throw new RuntimeException("权限不足，需角色：" + requiredRole);
        }
    }

    /**
     * 校验当前用户是否拥有目录操作权限（所有者或管理员）
     */
    public void checkFolderOwnership(Folder folder) {
        String currentUid = jwtUtil.getCurrentUsername();
        if (currentUid == null) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }

        if (!currentUid.equals(folder.getUid()) && !jwtUtil.isAdmin()) {
            throw new RuntimeException("越权访问：无权操作该目录");
        }
    }

    /**
     * 检查文件夹是否可编辑
     */
    public void checkFolderEditable(Folder folder) {
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new RuntimeException("系统目录不可编辑");
        }

        checkFolderOwnership(folder);
    }
}
