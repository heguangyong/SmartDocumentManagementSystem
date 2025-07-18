package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.*;
import com.github.sdms.repository.ShareAccessRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.ShareTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareAccessController {

    private final MinioService minioService;
    private final ShareAccessLogService shareAccessLogService;
    private final ShareAccessService shareAccessService;
    private final ShareAccessRepository shareAccessRepository;

    /**
     * 通过分享 Token 查看目录结构
     */
    @GetMapping("/view-folder")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(
            @RequestParam String token,
            @RequestParam String libraryCode
    ) {
        Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        List<Folder> children = shareAccessService.listChildFolders(folder);
        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children, folder.getLibraryCode());
        return ApiResponse.success("访问成功", result);
    }

    /**
     * 分享目录视图响应结构
     */
    public record SharedFolderView(String folderName, Long folderId, List<Folder> children, String libraryCode) {}

    /**
     * 分享链接下列出文件
     */
    @GetMapping("/files")
    @Operation(summary = "列出分享目录下的文件列表")
    public ApiResponse<List<UserFile>> listSharedFiles(
            @RequestParam String token,
            @RequestParam String libraryCode
    ) {
        Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        List<UserFile> files = shareAccessService.listFilesByFolder(folder);
        return ApiResponse.success("查询成功", files);
    }

    /**
     * 下载分享目录下的文件
     */
    @GetMapping("/download")
    @Operation(summary = "通过分享链接下载文件")
    public ResponseEntity<?> downloadFile(
            @RequestParam String token,
            @RequestParam Long fileId,
            @RequestParam String libraryCode,
            HttpServletRequest request
    ) {
        Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }

        UserFile file = shareAccessService.getFileByIdAndValidate(folder, fileId);
        if (file == null) {
            return ResponseEntity.status(403).body("该文件不属于当前分享目录");
        }

        // 记录访问日志，actionType 可扩展为 "download"
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");

        ShareAccessLog log = ShareAccessLog.builder()
                .token(token)
                .fileId(file.getId())
                .fileName(file.getOriginFilename())
                .accessIp(ip)
                .userAgent(ua)
                //.actionType("download")  // 如有该字段，请加上
                .build();
        shareAccessLogService.recordAccess(log);

        // 获取 MinIO 下载 URL 并重定向
        String url = minioService.getPresignedDownloadUrl(file.getBucket(), file.getUrl(), file.getOriginFilename());
        return ResponseEntity.status(302).header("Location", url).build();
    }

    /**
     * 统一创建分享链接
     */
    @PostMapping("/create")
    @Operation(summary = "统一创建分享链接")
    public ResponseEntity<?> createShare(
            @RequestParam String uid,
            @RequestParam String type,  // "file" 或 "folder"
            @RequestParam Long targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date expireAt
    ) {
        String token = shareAccessService.createShare(uid, type, targetId, expireAt);
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * 统一访问验证
     */
    @GetMapping("/access")
    @Operation(summary = "统一访问验证")
    public ResponseEntity<?> accessByToken(@RequestParam String token) {
        String tokenHash = DigestUtils.sha256Hex(token);
        ShareAccess share = shareAccessRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("分享链接无效"));

        if (!share.getActive() || (share.getExpireAt() != null && share.getExpireAt().before(new Date()))) {
            throw new RuntimeException("分享已失效");
        }

        return ResponseEntity.ok(share);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
