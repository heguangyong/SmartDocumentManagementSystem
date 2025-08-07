package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Folder;
import com.github.sdms.model.ShareAccess;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.ShareAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Tag(name = "共享访问", description = "文件/目录共享链接的生成与权限控制接口")
public class ShareAccessController {

    private final MinioService minioService;
    private final ShareAccessLogService shareAccessLogService;
    private final ShareAccessService shareAccessService;

    @GetMapping("/view-folder")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(@RequestParam String token, @RequestParam String libraryCode) {
        Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
        List<Folder> children = shareAccessService.listChildFolders(folder);
        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children, folder.getLibraryCode());
        return ApiResponse.success("访问成功", result);
    }

    public record SharedFolderView(String folderName, Long folderId, List<Folder> children, String libraryCode) {
    }

    @GetMapping("/files")
    @Operation(summary = "列出分享目录下的文件列表")
    public ApiResponse<List<UserFile>> listSharedFiles(@RequestParam String token, @RequestParam String libraryCode) {
        Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
        List<UserFile> files = shareAccessService.listFilesByFolder(folder);
        return ApiResponse.success("查询成功", files);
    }

    @GetMapping("/download")
    @Operation(summary = "通过分享链接下载文件（支持目录下的文件）")
    public ResponseEntity<?> downloadFile(@RequestParam String token, @RequestParam Long fileId, @RequestParam String libraryCode, HttpServletRequest request) {

        ShareAccess share = shareAccessService.getByToken(token, libraryCode);

        UserFile file;
        if ("file".equalsIgnoreCase(share.getTargetType())) {
            // 校验 fileId 是否与分享一致
            if (!Objects.equals(share.getTargetId(), fileId)) {
                return ResponseEntity.status(403).body("该文件不属于当前分享链接");
            }
            file = shareAccessService.getFileByToken(token, libraryCode);

        } else if ("folder".equalsIgnoreCase(share.getTargetType())) {
            Folder folder = shareAccessService.getFolderByToken(token, libraryCode);
            file = shareAccessService.getFileByIdAndValidate(folder, fileId);
            if (file == null) {
                return ResponseEntity.status(403).body("该文件不属于当前分享目录");
            }

        } else {
            return ResponseEntity.status(403).body("不支持的分享类型");
        }

        // 日志记录
        logAccess(request, token, file.getId(), file.getOriginFilename());

        // 跳转到 MinIO 预签名链接
        String url = minioService.getPresignedDownloadUrl(file.getBucket(), file.getUrl(), file.getOriginFilename());
        return ResponseEntity.status(302).header("Location", url).build();
    }

    @PostMapping("/create")
    @Operation(summary = "统一创建分享链接")
    public ResponseEntity<?> createShare(@RequestParam Long userId, @RequestParam String type, @RequestParam Long targetId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expireAt, @RequestParam String libraryCode) {
        // 设置默认过期时间（7天后）
        if (expireAt == null) {
            expireAt = LocalDateTime.now().plusDays(7);
        }

        String token = shareAccessService.createShare(userId, type, targetId, java.sql.Timestamp.valueOf(expireAt), libraryCode);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/access")
    @Operation(summary = "统一访问验证")
    public ResponseEntity<?> accessByToken(@RequestParam String token, @RequestParam String libraryCode) {
        ShareAccess share = shareAccessService.getByToken(token, libraryCode);
        if (share == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("分享不存在或已过期");
        }

        // 使用HashMap允许null值
        Map<String, Object> response = new HashMap<>();
        response.put("targetType", share.getTargetType());
        response.put("targetId", share.getTargetId());
        response.put("targetName", Optional.ofNullable(share.getTargetName()).orElse(""));
        response.put("expireAt", share.getExpireAt()); // 允许null值
        response.put("libraryCode", share.getLibraryCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销分享链接")
    public ResponseEntity<?> revokeShare(@RequestParam Long userId, @RequestParam String token, @RequestParam String libraryCode) {
        shareAccessService.revokeShare(userId, token, libraryCode);
        return ResponseEntity.ok("已撤销分享");
    }

    private void logAccess(HttpServletRequest request, String token, Long fileId, String filename) {
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");

        ShareAccessLog log = ShareAccessLog.builder().token(token).fileId(fileId).fileName(filename).accessIp(ip).userAgent(ua).actionType("download").build();
        shareAccessLogService.recordAccess(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader != null && !xfHeader.isBlank()) ? xfHeader.split(",")[0].trim() : request.getRemoteAddr();
    }
}
