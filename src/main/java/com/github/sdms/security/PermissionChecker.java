package com.github.sdms.security;

import com.github.sdms.components.JwtUtil;
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
}
