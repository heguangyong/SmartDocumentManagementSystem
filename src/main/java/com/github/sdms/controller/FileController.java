package com.github.sdms.controller;

import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.BucketPermission;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.repository.FilePermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.BucketUtil;
import com.github.sdms.util.CustomerUserDetails;
import com.github.sdms.util.PermissionChecker;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
    private final BucketPermissionRepository bucketPermissionRepository;
    private final UserFileRepository userFileRepository;

    @PreAuthorize("hasRole('ADMIN') or hasRole('LIBRARIAN') or hasRole('READER')")
    @Operation(summary = "文件列表")
    @PostMapping("/page")
    public ApiResponse<Page<UserFileSummaryDTO>> pageFiles(@RequestBody UserFilePageRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();

        Page<UserFileSummaryDTO> result = userFileService.pageFiles(request, userDetails);
        return ApiResponse.success(result);
    }




    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    @Operation(summary = "上传新文档", description = "上传文件到指定桶和目录，并返回文件信息")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserFileDTO> uploadNewDocument(
            @RequestPart MultipartFile file,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) Long bucketId,
            @RequestParam(required = false) String notes
    ) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 获取或创建可写桶
        Bucket targetBucket = getOrCreateWritableBucket(userId, libraryCode, bucketId);

        try {
            // 上传文件并返回 UserFile 实体
            UserFile savedFile = userFileService.uploadNewDocument(file, userId, targetBucket, notes, folderId);

            // 转换为 DTO 返回
            UserFileDTO dto = userFileService.toDTO(savedFile);
            return ApiResponse.success(dto);
        } catch (Exception e) {
            throw new ApiException(500, "文件上传失败：" + e.getMessage());
        }
    }

    private Bucket getOrCreateWritableBucket(Long userId, String libraryCode, Long bucketId) {
        Bucket targetBucket;

        if (bucketId != null) {
            targetBucket = bucketService.getBucketById(bucketId);
            if (targetBucket == null) {
                throw new ApiException(404, "目标桶不存在");
            }
            if (!permissionValidator.canWriteBucket(userId, targetBucket.getName())) {
                throw new ApiException(403, "您无权限上传至该桶：" + targetBucket.getName());
            }
        } else {
            String bucketName = BucketUtil.getBucketName(userId, libraryCode);
            targetBucket = bucketService.getOptionalBucketByName(bucketName)
                    .orElseGet(() -> {
                        // 创建默认桶
                        Bucket newBucket = Bucket.builder()
                                .name(bucketName)
                                .libraryCode(libraryCode)
                                .ownerId(userId)
                                .description("用户默认桶")
                                .build();
                        Bucket createdBucket = bucketService.createBucket(newBucket);

                        // 自动添加写权限
                        bucketPermissionRepository.save(
                                BucketPermission.builder()
                                        .userId(userId)
                                        .bucketId(createdBucket.getId())
                                        .permission("write")
                                        .createdAt(new Date())
                                        .build()
                        );
                        return createdBucket;
                    });

            if (!permissionValidator.canWriteBucket(userId, targetBucket.getName())) {
                throw new ApiException(403, "您没有该桶的写权限：" + targetBucket.getName());
            }
        }

        return targetBucket;
    }



    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    @Operation(summary = "批量上传新文档")
    @PostMapping("/upload-multiple")
    public ApiResponse<List<UserFile>> uploadMultipleDocuments(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @RequestParam List<MultipartFile> files,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) Long bucketId
    ) {
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();
        Bucket targetBucket;

        if (bucketId != null) {
            targetBucket = bucketService.getBucketById(bucketId);
            if (targetBucket == null) {
                throw new ApiException(404, "目标桶不存在");
            }

            if (!permissionValidator.canWriteBucket(userId, targetBucket.getName())) {
                throw new ApiException(403, "您无权限上传至该桶：" + targetBucket.getName());
            }

        } else {
            String bucketName = BucketUtil.getBucketName(userId, libraryCode);

            Optional<Bucket> optionalBucket = bucketService.getOptionalBucketByName(bucketName);
            if (optionalBucket.isEmpty()) {
                Bucket newBucket = Bucket.builder()
                        .name(bucketName)
                        .libraryCode(libraryCode)
                        .ownerId(userId)
                        .description("用户默认桶")
                        .build();
                targetBucket = bucketService.createBucket(newBucket);

                BucketPermission permission = BucketPermission.builder()
                        .userId(userId)
                        .bucketId(targetBucket.getId())
                        .permission("write")
                        .createdAt(new Date())
                        .build();
                bucketPermissionRepository.save(permission);
            } else {
                targetBucket = optionalBucket.get();

                if (!permissionValidator.canWriteBucket(userId, targetBucket.getName())) {
                    throw new ApiException(403, "您没有该桶的写权限：" + targetBucket.getName());
                }
            }
        }

        try {
            List<UserFile> uploadedList = userFileService.uploadMultipleNewDocuments(files, userId, targetBucket, notes, folderId);
            return ApiResponse.success(uploadedList);
        } catch (Exception e) {
            throw new ApiException(500, "批量文件上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/move")
    @Operation(summary = "移动文件到目标目录")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    public ApiResponse<Void> moveItems(@Valid @RequestBody MoveItemRequest request) {
        // 从 SecurityContext 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        // 调用业务逻辑
        userFileService.moveItems(request.getFileIds(), request.getFolderIds(), request.getTargetFolderId(), userId);

        return ApiResponse.success("移动成功", null);
    }


    @PostMapping("/copy")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    @Operation(summary = "复制文件到目标目录")
    public ApiResponse<UserFile> copyFile(@Valid @RequestBody CopyFileRequest request) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 调用业务逻辑（根据 fileId 查找原文件信息，做权限校验）
        UserFile copiedFile = userFileService.copyFile(
                request.getFileId(),
                userId,
                libraryCode,
                request.getTargetFolderId()
        );

        return ApiResponse.success(copiedFile);
    }




    @PostMapping("/uploadVersion")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    @Operation(summary = "上传文档新版本", description = "上传文档的新版本并返回最新版本信息")
    public ApiResponse<UserFileDTO> uploadNewVersion(
            @RequestParam MultipartFile file,
            @RequestParam Long docId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) Long bucketId
    ) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 如果外部没有传 folderId 或 bucketId，则用 docId 查询最新记录补齐
        if (folderId == null || bucketId == null) {
            UserFile latestFile = userFileRepository.findFirstByDocIdAndIsLatestAndLibraryCode(
                    docId, true, libraryCode
            ).orElseThrow(() -> new ApiException(404, "未找到该文档的最新记录"));

            if (folderId == null) {
                folderId = latestFile.getFolderId();
            }
            if (bucketId == null) {
                bucketId = latestFile.getBucketId();
            }
        }

        // 获取或创建可写桶
        Bucket targetBucket = getOrCreateWritableBucket(userId, libraryCode, bucketId);

        try {
            UserFile newVersion = userFileService.uploadNewVersion(file, userId, libraryCode, docId, notes, folderId, targetBucket);
            return ApiResponse.success(userFileService.toDTO(newVersion));
        } catch (Exception e) {
            throw new ApiException(500, "上传失败：" + e.getMessage());
        }
    }




    @PostMapping("/versions")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取指定文档所有版本", description = "按版本号降序返回所有文件版本，并标识最新版本")
    public ApiResponse<List<UserFileDTO>> getAllVersions(@Valid @RequestBody UserFileVersionRequest request) {

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 权限校验
        permissionChecker.checkAccess(userId, libraryCode);

        // 查询所有版本
        List<UserFileDTO> versions = userFileService.getVersionsByDocId(
                request.getDocId(),
                libraryCode,
                request.getBucketId(),
                request.getFolderId()
        );
        return ApiResponse.success(versions);
    }


    @GetMapping("/list")
    @Operation(summary = "获取当前用户文件列表")
    public ApiResponse<List<UserFile>> list(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());
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
                UserFile file = userFileService.getFileByName(filename, userDetails.getUserId(), userDetails.getLibraryCode());

                // 校验文件权限：确保用户有删除权限
                permissionChecker.checkFileAccess(userDetails.getUserId(), file.getId(), "DELETE");

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());
        userFileService.restoreFiles(userDetails.getUserId(), filenames, userDetails.getLibraryCode());
        return ApiResponse.success("文件已恢复", null);
    }

    @GetMapping("/download/{fileId}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ApiResponse<Map<String, String>> download(@PathVariable Long fileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();

        // 获取文件（内部已做权限校验）
        UserFile file = userFileService.getFileById(fileId);

        // 生成 MinIO 签名下载链接，使用文件实际桶名
        String downloadUrl = minioService.generatePresignedDownloadUrl(
                userDetails.getUserId(),
                userDetails.getLibraryCode(),
                file.getName(),
                file.getBucket()
        );

        Map<String, String> result = new HashMap<>();
        result.put("downloadUrl", downloadUrl);
        result.put("filename", file.getOriginFilename());

        return ApiResponse.success("获取下载链接成功", result);
    }



    @GetMapping("/usage")
    @Operation(summary = "获取当前用户已使用空间（单位：字节）")
    public ApiResponse<Long> getUserStorageUsage(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());
        long usage = userFileService.listFilesByRole(userDetails).stream()
                .mapToLong(UserFile::getSize)
                .sum();
        return ApiResponse.success(usage);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取当前用户最近删除的文件（7天内）")
    public ApiResponse<List<UserFile>> getDeletedFiles(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());
        List<UserFile> deletedFiles = userFileService.getDeletedFilesWithin7Days(userDetails.getUserId(), userDetails.getLibraryCode());
        return ApiResponse.success(deletedFiles);
    }

    @GetMapping("/presigned-url/{filename}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Operation(summary = "获取指定文件的临时下载链接")
    public ApiResponse<String> getPresignedUrl(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                               @PathVariable String filename) {
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

        List<UserFile> deletedFiles = userFileService.getDeletedFilesWithin7Days(userDetails.getUserId(), userDetails.getLibraryCode());
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
        permissionChecker.checkAccess(userDetails.getUserId(), userDetails.getLibraryCode());

        long used = userFileService.getUserStorageUsage(userDetails.getUserId(), userDetails.getLibraryCode());
        long max = storageQuotaService.getMaxQuota(userDetails.getUserId(), userDetails.getLibraryCode());
        long remaining = max - used;

        Map<String, Long> result = Map.of(
                "used", used,
                "max", max,
                "remaining", remaining
        );

        return ApiResponse.success("配额信息", result);
    }

}
