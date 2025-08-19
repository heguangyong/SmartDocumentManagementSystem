package com.github.sdms.util;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.*;
import com.github.sdms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PermissionChecker {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FilePermissionRepository filePermissionRepository;

    @Autowired
    private UserFileRepository userFileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private BucketPermissionRepository bucketPermissionRepository;

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

        UserFile file = userFileRepository.findById(fileId).orElseThrow(() -> new ApiException(404, "文件不存在"));

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

    /**
     * 检查文件夹访问权限
     */
    public void checkFolderAccess(Long userId, Long folderId, String libraryCode) {
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new ApiException(404, "文件夹不存在"));

        // 检查用户是否有权限访问该文件夹
        if (!folder.getUserId().equals(userId) || !folder.getLibraryCode().equals(libraryCode)) {
            throw new ApiException(403, "无权限访问该文件夹");
        }
    }

    /**
     * 检查存储桶访问权限
     */
    public void checkBucketAccess(Long userId, Long bucketId, String libraryCode) {
        // 首先检查存储桶是否存在
        Bucket bucket = bucketRepository.findById(bucketId).orElseThrow(() -> new ApiException(404, "存储桶不存在"));

        // 检查库代码是否匹配（如果 Bucket 实体有 libraryCode 字段）
        if (bucket.getLibraryCode() != null && !bucket.getLibraryCode().equals(libraryCode)) {
            throw new ApiException(403, "无权限访问该存储桶");
        }

        // 检查用户是否有该存储桶的访问权限
        BucketPermission permission = bucketPermissionRepository.findByUserIdAndBucketId(userId, bucketId).orElseThrow(() -> new ApiException(403, "无权限访问该存储桶"));

        // 验证权限是否有效（可以根据需要添加更细粒度的权限检查）
        if (permission.getPermission() == null || permission.getPermission().trim().isEmpty()) {
            throw new ApiException(403, "无有效权限访问该存储桶");
        }
    }

    /**
     * 检查存储桶的特定权限（读取、写入、管理等）
     */
    public void checkBucketPermission(Long userId, Long bucketId, String libraryCode, String requiredPermission) {
        // 首先进行基本的访问权限检查
        checkBucketAccess(userId, bucketId, libraryCode);

        // 获取用户权限
        BucketPermission permission = bucketPermissionRepository
                .findByUserIdAndBucketId(userId, bucketId)
                .orElseThrow(() -> new ApiException(403, "无权限访问该存储桶"));

        String userPermissions = permission.getPermission();

        // 检查是否有管理员权限（管理员权限包含所有权限）
        if (userPermissions.contains("admin")) {
            return;
        }

        // 检查是否有所需的具体权限
        if (!userPermissions.contains(requiredPermission)) {
            throw new ApiException(403, "权限不足，需要 " + requiredPermission + " 权限");
        }
    }

    /**
     * 检查用户是否有存储桶的读取权限
     */
    public void checkBucketReadPermission(Long userId, Long bucketId, String libraryCode) {
        checkBucketPermission(userId, bucketId, libraryCode, "read");
    }

    /**
     * 检查用户是否有存储桶的写入权限
     */
    public void checkBucketWritePermission(Long userId, Long bucketId, String libraryCode) {
        checkBucketPermission(userId, bucketId, libraryCode, "write");
    }

    /**
     * 检查用户是否有存储桶的管理权限
     */
    public void checkBucketAdminPermission(Long userId, Long bucketId, String libraryCode) {
        checkBucketPermission(userId, bucketId, libraryCode, "admin");
    }
}
