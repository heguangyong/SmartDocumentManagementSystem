package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.Folder;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.FolderService;
import com.github.sdms.service.MinioClientService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.ShareTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/public/folder")
@RequiredArgsConstructor
public class PublicFolderController {

    private final FolderService folderService;
    private final UserFileService userFileService;
    private final MinioClientService minioClientService;
    private final ShareAccessLogService shareAccessLogService;

    @GetMapping("/view")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(
            @RequestParam String token
    ) {
        Folder folder = folderService.getFolderByShareToken(token);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        // 可选扩展：返回该目录下的文件或子目录（只读）
        List<Folder> children = folderService.listFolders(folder.getOwnerId(), folder.getId());

        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children);
        return ApiResponse.success("访问成功", result);
    }

    public record SharedFolderView(String folderName, Long folderId, List<Folder> children) {}

    @GetMapping("/files")
    @Operation(summary = "列出分享目录下的文件列表")
    public ApiResponse<List<UserFile>> listSharedFiles(@RequestParam String token) {
        Folder folder = folderService.getFolderByShareToken(token);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ApiResponse.failure(e.getMessage());
        }

        // 查询该目录下的文件列表（只读）
        List<UserFile> files = userFileService.listFilesByFolder(folder.getOwnerId(), folder.getId());
        return ApiResponse.success("查询成功", files);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(
            @RequestParam String token,
            @RequestParam Long fileId,
            HttpServletRequest request
    ) {
        Folder folder = folderService.getFolderByShareToken(token);
        try {
            ShareTokenValidator.validateShareToken(folder);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }

        UserFile file = userFileService.getFileById(fileId);
        if (!file.getFolderId().equals(folder.getId())) {
            return ResponseEntity.status(403).body("该文件不属于当前分享目录");
        }

        // ✅ 记录访问日志
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

        // ✅ 跳转至 MinIO 下载链接
        String url = minioClientService.getPresignedDownloadUrl(file.getBucket(), file.getUrl(), file.getOriginFilename());
        return ResponseEntity.status(302).header("Location", url).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

}
