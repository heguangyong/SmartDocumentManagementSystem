package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.UserFile;
import com.github.sdms.util.PermissionChecker;
import com.github.sdms.service.MinioClientService;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/userFile")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileService userFileService;
    private final MinioClientService minioClientService;
    private final PermissionChecker permissionChecker;
    private final StorageQuotaService storageQuotaService;

    @GetMapping("/{uid}/list")
    @Operation(summary = "获取用户文件列表（所有用户，受用户身份校验限制）")
    public ApiResponse<List<UserFile>> list(@PathVariable String uid) {
        permissionChecker.checkAccess(uid);
        return ApiResponse.success(userFileService.getActiveFiles(uid));
    }

    @DeleteMapping("/{uid}/delete")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "逻辑删除用户文件（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> deleteFiles(@PathVariable String uid, @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(uid);
        userFileService.softDeleteFiles(uid, filenames);
        return ApiResponse.success("文件已删除", null);
    }

    @PostMapping("/{uid}/restore")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "恢复最近删除的文件（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> restoreFiles(@PathVariable String uid, @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(uid);
        userFileService.restoreFiles(uid, filenames);
        return ApiResponse.success("文件已恢复", null);
    }

    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/download/{uid}/{filename}")
    @Operation(summary = "下载用户文件（读者READER及以上）")
    public void download(@PathVariable String uid,
                         @PathVariable String filename,
                         HttpServletResponse response) {
        permissionChecker.checkAccess(uid);

        try {
            UserFile file = userFileService.getActiveFiles(uid).stream()
                    .filter(f -> f.getName().equals(filename))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("文件不存在"));

            response.setContentType(file.getType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getOriginFilename() + "\"");

            try (InputStream is = minioClientService.getObject(file.getBucket(), file.getName())) {
                is.transferTo(response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{uid}/usage")
    @Operation(summary = "获取用户已使用空间（单位：字节）（所有用户，受用户身份校验限制）")
    public ApiResponse<Long> getUserStorageUsage(@PathVariable String uid) {
        permissionChecker.checkAccess(uid);
        long usage = userFileService.getActiveFiles(uid).stream()
                .mapToLong(UserFile::getSize)
                .sum();
        return ApiResponse.success(usage);
    }

    @GetMapping("/{uid}/deleted")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取用户最近删除的文件（7天内）（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<List<UserFile>> getDeletedFiles(@PathVariable String uid) {
        permissionChecker.checkAccess(uid);
        return ApiResponse.success(userFileService.getDeletedFilesWithin7Days(uid));
    }

    @GetMapping("/presigned-url/{uid}/{filename}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取指定文件的临时下载链接（读者READER及以上）")
    public ApiResponse<String> getPresignedUrl(@PathVariable String uid, @PathVariable String filename) {
        permissionChecker.checkAccess(uid);

        UserFile file = userFileService.getActiveFiles(uid).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        String url = minioClientService.getPresignedUrl(file.getBucket(), file.getName());
        return ApiResponse.success(url);
    }

    @GetMapping("/{uid}/info/{filename}")
    @Operation(summary = "获取指定文件详情（所有用户，受用户身份校验限制）")
    public ApiResponse<UserFile> getFileInfo(@PathVariable String uid, @PathVariable String filename) {
        permissionChecker.checkAccess(uid);
        UserFile file = userFileService.getActiveFiles(uid).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        return ApiResponse.success(file);
    }

    @PostMapping("/{uid}/batchInfo")
    @Operation(summary = "批量获取文件详情（所有用户，受用户身份校验限制）")
    public ApiResponse<List<UserFile>> getBatchFileInfo(@PathVariable String uid, @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(uid);
        List<UserFile> files = userFileService.getActiveFiles(uid).stream()
                .filter(f -> filenames.contains(f.getName()))
                .toList();
        return ApiResponse.success(files);
    }

    @PostMapping("/{uid}/rename")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "重命名文件（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> renameFile(@PathVariable String uid,
                                        @RequestParam String oldName,
                                        @RequestParam String newName) {
        permissionChecker.checkAccess(uid);

        List<UserFile> files = userFileService.getActiveFiles(uid);
        UserFile file = files.stream()
                .filter(f -> f.getName().equals(oldName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        boolean exists = files.stream().anyMatch(f -> f.getName().equals(newName));
        if (exists) throw new RuntimeException("新文件名已存在");

        file.setName(newName);
        userFileService.saveUserFile(file);

        return ApiResponse.success("文件已重命名", null);
    }

    @DeleteMapping("/{uid}/purgeFile")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "彻底删除指定文件（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> purgeFile(@PathVariable String uid, @RequestParam String filename) {
        permissionChecker.checkAccess(uid);

        UserFile file = userFileService.getActiveFiles(uid).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        minioClientService.deleteObject(file.getBucket(), file.getName());
        userFileService.deletePermanently(file.getId());

        return ApiResponse.success("文件已彻底删除", null);
    }

    @GetMapping("/{uid}/type/{fileType}")
    @Operation(summary = "按文件类型查询（如 image/png）（所有用户，受用户身份校验限制）")
    public ApiResponse<List<UserFile>> getFilesByType(@PathVariable String uid,
                                                      @PathVariable String fileType) {
        permissionChecker.checkAccess(uid);
        List<UserFile> files = userFileService.getActiveFiles(uid).stream()
                .filter(f -> fileType.equalsIgnoreCase(f.getType()))
                .toList();
        return ApiResponse.success(files);
    }

    @DeleteMapping("/{uid}/purgeFiles")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "批量物理删除用户文件（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> purgeFiles(@PathVariable String uid, @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(uid);

        List<UserFile> files = userFileService.getActiveFiles(uid).stream()
                .filter(f -> filenames.contains(f.getName()))
                .toList();

        for (UserFile file : files) {
            minioClientService.deleteObject(file.getBucket(), file.getName());
            userFileService.deletePermanently(file.getId());
        }

        return ApiResponse.success("已永久删除选中文件", null);
    }

    @DeleteMapping("/{uid}/trash/empty")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "清空当前用户回收站（彻底删除软删除文件）（馆员LIBRARIAN及管理员ADMIN）")
    public ApiResponse<Void> emptyTrash(@PathVariable String uid) {
        permissionChecker.checkAccess(uid);

        List<UserFile> deletedFiles = userFileService.getDeletedFilesWithin7Days(uid);
        for (UserFile file : deletedFiles) {
            try {
                minioClientService.deleteObject(file.getBucket(), file.getName());
            } catch (Exception ignored) {
            }
            userFileService.deletePermanently(file.getId());
        }

        return ApiResponse.success("回收站已清空", null);
    }

    @DeleteMapping("/admin/purge-expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员清理过期删除记录（超出7天）（仅限管理员ADMIN）")
    public ApiResponse<Void> purgeExpiredFiles() {
        Date cutoff = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);

        List<UserFile> expiredFiles = userFileService.getDeletedFilesBefore(cutoff);

        for (UserFile file : expiredFiles) {
            try {
                minioClientService.deleteObject(file.getBucket(), file.getName());
            } catch (Exception ignored) {
            }
        }

        userFileService.deleteFiles(expiredFiles);

        return ApiResponse.success("过期资源已全部清理", null);
    }

    @GetMapping("/{uid}/quota")
    @Operation(summary = "获取用户存储配额信息（所有用户，受用户身份校验限制）")
    public ApiResponse<Map<String, Long>> getQuota(@PathVariable String uid) {
        permissionChecker.checkAccess(uid);
        long used = userFileService.getUserStorageUsage(uid);
        long max = storageQuotaService.getMaxQuota(uid);
        long remaining = max - used;

        Map<String, Long> result = Map.of(
                "used", used,
                "max", max,
                "remaining", remaining
        );

        return ApiResponse.success("配额信息", result);
    }

}
