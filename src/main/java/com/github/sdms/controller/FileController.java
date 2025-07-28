package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.FilePermissionRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.CustomerUserDetails;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final UserFileService userFileService;
    private final MinioService minioService;
    private final BucketService bucketService;
    private final PermissionChecker permissionChecker;
    private final PermissionValidator permissionValidator;
    private final StorageQuotaService storageQuotaService;
    private final FilePermissionRepository filePermissionRepository;


    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    @Operation(summary = "上传新文档")
    @PostMapping("/upload")
    public ApiResponse<UserFile> uploadNewDocument(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) Long bucketId
    ) {
        String uid = userDetails.getUid();
        Bucket targetBucket;

        if (bucketId != null) {
            targetBucket = bucketService.getBucketById(bucketId);
            if (targetBucket == null) {
                throw new ApiException(404, "目标桶不存在");
            }

            String bucketName = targetBucket.getName();
            if (!permissionValidator.canWriteBucket(uid, bucketName)) {
                throw new ApiException(403, "您无权限上传至该桶：" + bucketName);
            }
        } else {
            targetBucket = bucketService.getUserDefaultBucket(uid, userDetails.getLibraryCode());
            if (targetBucket == null) {
                throw new ApiException(403, "您没有默认上传桶，请联系管理员");
            }
        }

        try {
            UserFile savedFile = userFileService.uploadNewDocument(file, uid, targetBucket, notes, folderId);
            return ApiResponse.success(savedFile);
        } catch (Exception e) {
            throw new ApiException(500, "文件上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/uploadVersion")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "上传文档新版本")
    public ApiResponse<UserFile> uploadNewVersion(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @RequestParam MultipartFile file,
            @RequestParam Long docId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long folderId // 👈 新增目录ID参数
    ) {
        try {
            UserFile newVersion = userFileService.uploadNewVersion(file, userDetails.getUid(), userDetails.getLibraryCode(), docId, notes,folderId);
            return ApiResponse.success(newVersion);
        } catch (Exception e) {
            log.error("上传文档新版本失败", e);
            return ApiResponse.failure("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/versions/{docId}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取指定文档所有版本")
    public ApiResponse<List<UserFile>> getAllVersions(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @PathVariable Long docId
    ) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());
        List<UserFile> versions = userFileService.getVersionsByDocId(docId, userDetails.getLibraryCode());
        return ApiResponse.success(versions);
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前用户文件列表")
    public ApiResponse<List<UserFile>> list(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());
        List<UserFile> files = userFileService.listFilesByRole(userDetails);
        return ApiResponse.success(files);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "逻辑删除当前用户文件")
    public ApiResponse<Void> deleteFiles(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                         @RequestBody List<String> filenames) {
        try {
            for (String filename : filenames) {
                // 查找文件
                UserFile file = userFileService.getFileByName(filename, userDetails.getUid(), userDetails.getLibraryCode());

                // 校验文件权限：确保用户有删除权限
                permissionChecker.checkFileAccess(userDetails.getUid(), file.getId(), "DELETE");

                // 删除文件逻辑
                userFileService.softDeleteFile(file);
            }
            return ApiResponse.success("文件已删除", null);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return ApiResponse.failure("删除失败: " + e.getMessage());
        }
    }


    @PostMapping("/restore")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "恢复最近删除的文件")
    public ApiResponse<Void> restoreFiles(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                          @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());
        userFileService.restoreFiles(userDetails.getUid(), filenames, userDetails.getLibraryCode());
        return ApiResponse.success("文件已恢复", null);
    }

    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "下载当前用户文件")
    public void download(@AuthenticationPrincipal CustomerUserDetails userDetails,
                         @PathVariable String filename,
                         HttpServletResponse response) {
        try {
            // 查找文件
            UserFile file = userFileService.getFileByName(filename, userDetails.getUid(), userDetails.getLibraryCode());

            // 校验文件权限：确保用户有下载权限
            permissionChecker.checkFileAccess(userDetails.getUid(), file.getId(), "READ");

            // 文件下载逻辑
            response.setContentType(file.getType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getOriginFilename() + "\"");

            try (InputStream is = minioService.getObject(file.getBucket(), file.getName())) {
                is.transferTo(response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            log.error("文件下载失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/usage")
    @Operation(summary = "获取当前用户已使用空间（单位：字节）")
    public ApiResponse<Long> getUserStorageUsage(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());
        long usage = userFileService.listFilesByRole(userDetails).stream()
                .mapToLong(UserFile::getSize)
                .sum();
        return ApiResponse.success(usage);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取当前用户最近删除的文件（7天内）")
    public ApiResponse<List<UserFile>> getDeletedFiles(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());
        List<UserFile> deletedFiles = userFileService.getDeletedFilesWithin7Days(userDetails.getUid(), userDetails.getLibraryCode());
        return ApiResponse.success(deletedFiles);
    }

    @GetMapping("/presigned-url/{filename}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取指定文件的临时下载链接")
    public ApiResponse<String> getPresignedUrl(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                               @PathVariable String filename) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        UserFile file = userFileService.listFilesByRole(userDetails).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        String url = minioService.getPresignedUrl(file.getBucket(), file.getName());
        return ApiResponse.success(url);
    }

    @GetMapping("/info/{filename}")
    @Operation(summary = "获取指定文件详情")
    public ApiResponse<UserFile> getFileInfo(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                             @PathVariable String filename) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        UserFile file = userFileService.listFilesByRole(userDetails).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        return ApiResponse.success(file);
    }

    @PostMapping("/batchInfo")
    @Operation(summary = "批量获取文件详情")
    public ApiResponse<List<UserFile>> getBatchFileInfo(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                                        @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        List<UserFile> files = userFileService.listFilesByRole(userDetails).stream()
                .filter(f -> filenames.contains(f.getName()))
                .toList();
        return ApiResponse.success(files);
    }

    @PostMapping("/rename")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "重命名文件")
    public ApiResponse<Void> renameFile(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                        @RequestParam String oldName,
                                        @RequestParam String newName) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        List<UserFile> files = userFileService.listFilesByRole(userDetails);
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

    @DeleteMapping("/purgeFile")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "彻底删除指定文件")
    public ApiResponse<Void> purgeFile(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                       @RequestParam String filename) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        UserFile file = userFileService.listFilesByRole(userDetails).stream()
                .filter(f -> f.getName().equals(filename))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        minioService.deleteObject(file.getBucket(), file.getName());
        userFileService.deletePermanently(file.getId(), userDetails.getLibraryCode());

        return ApiResponse.success("文件已彻底删除", null);
    }

    @DeleteMapping("/purgeFiles")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "批量物理删除用户文件")
    public ApiResponse<Void> purgeFiles(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                        @RequestBody List<String> filenames) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        List<UserFile> files = userFileService.listFilesByRole(userDetails).stream()
                .filter(f -> filenames.contains(f.getName()))
                .toList();

        for (UserFile file : files) {
            minioService.deleteObject(file.getBucket(), file.getName());
            userFileService.deletePermanently(file.getId(), userDetails.getLibraryCode());
        }

        return ApiResponse.success("已永久删除选中文件", null);
    }

    @DeleteMapping("/trash/empty")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "清空当前用户回收站")
    public ApiResponse<Void> emptyTrash(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        List<UserFile> deletedFiles = userFileService.getDeletedFilesWithin7Days(userDetails.getUid(), userDetails.getLibraryCode());
        for (UserFile file : deletedFiles) {
            try {
                minioService.deleteObject(file.getBucket(), file.getName());
            } catch (Exception ignored) {
            }
            userFileService.deletePermanently(file.getId(), userDetails.getLibraryCode());
        }

        return ApiResponse.success("回收站已清空", null);
    }

    @DeleteMapping("/admin/purge-expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员清理过期删除记录（超出7天）")
    public ApiResponse<Void> purgeExpiredFiles(@RequestParam String libraryCode) {
        Date cutoff = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);

        List<UserFile> expiredFiles = userFileService.getDeletedFilesBefore(cutoff, libraryCode);

        for (UserFile file : expiredFiles) {
            try {
                minioService.deleteObject(file.getBucket(), file.getName());
            } catch (Exception ignored) {
            }
        }

        userFileService.deleteFiles(expiredFiles);

        return ApiResponse.success("过期资源已全部清理", null);
    }

    @GetMapping("/quota")
    @Operation(summary = "获取当前用户存储配额信息")
    public ApiResponse<Map<String, Long>> getQuota(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUid(), userDetails.getLibraryCode());

        long used = userFileService.getUserStorageUsage(userDetails.getUid(), userDetails.getLibraryCode());
        long max = storageQuotaService.getMaxQuota(userDetails.getUid(), userDetails.getLibraryCode());
        long remaining = max - used;

        Map<String, Long> result = Map.of(
                "used", used,
                "max", max,
                "remaining", remaining
        );

        return ApiResponse.success("配额信息", result);
    }

}
