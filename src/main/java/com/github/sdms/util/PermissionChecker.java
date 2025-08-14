package com.github.sdms.util;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.FilePermission;
import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.FilePermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final JwtUtil jwtUtil;
    private final FilePermissionRepository filePermissionRepository;
    private final UserFileRepository userFileRepository;

    public void checkFileAccess(Long targetUserId, Long fileId, String requiredPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(401, "无效会话：无法识别当前登录用户");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomerUserDetails userDetails)) {
            throw new ApiException(401, "当前会话用户无效");
        }

        Long currentUserId = userDetails.getUser().getId();
        String currentRole = userDetails.getUser().getRoleType().toString();
        String currentLibraryCode = userDetails.getLibraryCode();

        if (currentUserId == null || currentRole == null || currentLibraryCode == null) {
            throw new ApiException(401, "当前用户信息不完整，无法进行权限校验");
        }

        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(404, "文件不存在"));

        // 在文件权限校验中增加管理员放行
        if ("ADMIN".equals(currentRole)) {
            return; // 管理员默认拥有全部权限
        }

        FilePermission filePermission = filePermissionRepository.findByUserAndFile(userDetails.getUser(), file);
        if (filePermission == null) {
            throw new ApiException(403, "用户没有访问该文件的权限");
        }

        if (!filePermission.getPermission().equals(requiredPermission)) {
            throw new ApiException(403, "权限不足，无法执行此操作");
        }
    }

    public void checkAccess(Long targetUserId, String targetLibraryCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(401, "无效会话：无法识别当前登录用户");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomerUserDetails userDetails)) {
            throw new ApiException(401, "当前会话用户无效");
        }

        Long currentUserId = userDetails.getUser().getId();
        String currentRole = userDetails.getUser().getRoleType().toString();
        String currentLibraryCode = userDetails.getLibraryCode();

        if (currentUserId == null || currentRole == null || currentLibraryCode == null) {
            throw new ApiException(401, "当前用户信息不完整，无法进行权限校验");
        }

        switch (currentRole) {
            case "ADMIN":
                return;
            case "LIBRARIAN":
                if (!currentLibraryCode.equals(targetLibraryCode)) {
                    throw new ApiException(403, "越权访问：不能访问其他馆点数据");
                }
                return;
            case "READER":
                if (!currentUserId.equals(targetUserId) || !currentLibraryCode.equals(targetLibraryCode)) {
                    throw new ApiException(403, "越权访问：无权访问其他用户或其他馆点资源");
                }
                return;
            default:
                throw new ApiException(400, "未识别的角色：" + currentRole);
        }
    }

    public void checkAccess(Long targetUserId) {
        Long currentUserId = JwtUtil.getCurrentUserIdOrThrow();
        if (currentUserId == null) {
            throw new ApiException(401, "无效会话：无法识别当前登录用户");
        }
        if (!Objects.equals(targetUserId, currentUserId) && !jwtUtil.isAdmin()) {
            throw new ApiException(403, "越权访问：无权访问其他用户资源");
        }
    }

    public void checkFolderOwnership(Folder folder) {
        Long currentUserId = JwtUtil.getCurrentUserIdOrThrow();
        if (currentUserId == null) {
            throw new ApiException(401, "无效会话：无法识别当前登录用户");
        }
        if (!currentUserId.equals(folder.getUserId()) && !jwtUtil.isAdmin()) {
            throw new ApiException(403, "越权访问：无权操作该目录");
        }
    }

    public void checkFolderEditable(Folder folder) {
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new ApiException(403, "系统目录不可编辑");
        }
        checkFolderOwnership(folder);
    }

    public void requireAdmin() {
        if (!jwtUtil.isAdmin()) {
            throw new ApiException(403, "只有管理员才能执行该操作");
        }
    }

    public void requireRole(String requiredRole) {
        String currentRole = jwtUtil.getCurrentRole();
        if (currentRole == null || !currentRole.equalsIgnoreCase(requiredRole)) {
            throw new ApiException(403, "权限不足，需角色：" + requiredRole);
        }
    }
}
