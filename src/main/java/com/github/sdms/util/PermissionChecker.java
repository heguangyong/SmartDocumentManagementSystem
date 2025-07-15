package com.github.sdms.util;

import com.github.sdms.model.Folder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final JwtUtil jwtUtil;

    /**
     * 检查当前登录用户是否可以访问指定 targetUid 和 targetLibraryCode 的资源
     * 角色权限逻辑：
     * - ADMIN：无条件访问所有资源
     * - LIBRARIAN：只能访问本馆资源（馆代码一致）
     * - READER：只能访问本人且馆代码一致
     *
     * @param targetUid 目标用户UID
     * @param targetLibraryCode 目标馆代码
     */
    public void checkAccess(String targetUid, String targetLibraryCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomerUserDetails userDetails)) {
            throw new RuntimeException("当前会话用户无效");
        }

        String currentUid = userDetails.getUid();
        String currentRole = userDetails.getUser().getRole().toString();  // 直接从 AppUser 拿角色
        String currentLibraryCode = userDetails.getLibraryCode();  // ✅ 可靠方式

        if (currentUid == null || currentRole == null || currentLibraryCode == null) {
            throw new RuntimeException("当前用户信息不完整，无法进行权限校验");
        }

        switch (currentRole) {
            case "ADMIN":
                return;
            case "LIBRARIAN":
                if (!currentLibraryCode.equals(targetLibraryCode)) {
                    throw new RuntimeException("越权访问：不能访问其他馆点数据");
                }
                return;
            case "READER":
                if (!currentUid.equals(targetUid) || !currentLibraryCode.equals(targetLibraryCode)) {
                    throw new RuntimeException("越权访问：无权访问其他用户或其他馆点资源");
                }
                return;
            default:
                throw new RuntimeException("未识别的角色：" + currentRole);
        }
    }


    /**
     * 兼容旧版的 checkAccess，仅判断当前登录用户是否为目标用户或管理员
     */
    public void checkAccess(String targetUid) {
        String currentUid = jwtUtil.getCurrentUsername();
        if (currentUid == null) {
            throw new RuntimeException("无效会话：无法识别当前登录用户");
        }
        if (!Objects.equals(targetUid, currentUid) && !jwtUtil.isAdmin()) {
            throw new RuntimeException("越权访问：无权访问其他用户资源");
        }
    }

    /**
     * 强制要求当前用户为管理员
     */
    public void requireAdmin() {
        if (!jwtUtil.isAdmin()) {
            throw new RuntimeException("只有管理员才能执行该操作");
        }
    }

    /**
     * 强制要求当前用户拥有指定角色
     * @param requiredRole 角色名（大小写不敏感）
     */
    public void requireRole(String requiredRole) {
        String currentRole = jwtUtil.getCurrentRole();
        if (currentRole == null || !currentRole.equalsIgnoreCase(requiredRole)) {
            throw new RuntimeException("权限不足，需角色：" + requiredRole);
        }
    }

    /**
     * 校验当前用户是否为目录所有者或管理员
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
     * 校验目录是否可编辑，系统目录不可编辑
     */
    public void checkFolderEditable(Folder folder) {
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new RuntimeException("系统目录不可编辑");
        }
        checkFolderOwnership(folder);
    }
}
