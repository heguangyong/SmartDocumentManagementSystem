package com.github.sdms.security;

import com.github.sdms.components.JwtUtil;
import com.github.sdms.model.Folder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final JwtUtil jwtUtil;

    /**
     * 检查当前用户是否可以访问指定 UID 的资源
     * - 当前用户为本人或管理员可访问
     * - 否则抛出越权异常
     */
    public void checkAccess(String targetUid) {
        String currentUid = jwtUtil.getCurrentUsername();
        if (currentUid == null) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }

        boolean isAdmin = jwtUtil.isAdmin();

        if (!targetUid.equals(currentUid) && !isAdmin) {
            throw new RuntimeException("越权访问：无权访问其他用户资源");
        }
    }

    /**
     * 检查当前用户是否具备指定角色
     */
    public void requireRole(String requiredRole) {
        String role = jwtUtil.getCurrentRole();
        if (!requiredRole.equals(role)) {
            throw new RuntimeException("权限不足，需角色：" + requiredRole);
        }
    }

    /**
     * 检查是否是管理员
     */
    public void requireAdmin() {
        if (!jwtUtil.isAdmin()) {
            throw new RuntimeException("只有管理员才能执行该操作");
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
     * 校验当前用户是否可编辑此目录
     * - 系统目录不可编辑
     * - 非本人且非管理员不可操作
     */
    public void checkFolderEditable(Folder folder) {
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new RuntimeException("系统目录不可编辑");
        }

        checkFolderOwnership(folder);
    }


}
