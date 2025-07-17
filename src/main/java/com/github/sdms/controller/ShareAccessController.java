package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Folder;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.FolderService;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.ShareTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareAccessController {

    private final FolderService folderService;
    private final UserFileService userFileService;
    private final MinioService minioService;
    private final ShareAccessLogService shareAccessLogService;

    /**
     * 通过分享 Token 查看目录结构
     */
    @GetMapping("/view-folder")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(
            @RequestParam String token,
            @RequestParam String libraryCode
    ) {
        Folder folder = folderService.getFolderByShareToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        List<Folder> children = folderService.listFolders(folder.getOwnerId(), folder.getId(), libraryCode);
        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children);
        return ApiResponse.success("访问成功", result);
    }

    /**
     * 分享目录视图响应结构
     */
    public record SharedFolderView(String folderName, Long folderId, List<Folder> children) {}

    /**
     * 分享链接下列出文件
     */
    @GetMapping("/files")
    @Operation(summary = "列出分享目录下的文件列表")
    public ApiResponse<List<UserFile>> listSharedFiles(
            @RequestParam String token,
            @RequestParam String libraryCode
    ) {
        Folder folder = folderService.getFolderByShareToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        List<UserFile> files = userFileService.listFilesByFolder(folder.getOwnerId(), folder.getId(), libraryCode);
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
        Folder folder = folderService.getFolderByShareToken(token, libraryCode);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }

        UserFile file = userFileService.getFileById(fileId, libraryCode);
        if (!file.getFolderId().equals(folder.getId())) {
            return ResponseEntity.status(403).body("该文件不属于当前分享目录");
        }

        // 记录访问日志
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");

        ShareAccessLog log = ShareAccessLog.builder()
                .token(token)
                .fileId(file.getId())
                .fileName(file.getOriginFilename())
                .accessIp(ip)
                .userAgent(ua)
                .build();
        shareAccessLogService.recordAccess(log);

        // 获取 MinIO 下载 URL 并重定向
        String url = minioService.getPresignedDownloadUrl(file.getBucket(), file.getUrl(), file.getOriginFilename());
        return ResponseEntity.status(302).header("Location", url).build();
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
