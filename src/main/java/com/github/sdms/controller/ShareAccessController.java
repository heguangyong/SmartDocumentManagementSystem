package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.ShareCreateReqVO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Folder;
import com.github.sdms.model.ShareAccess;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.ShareAccessService;
import com.github.sdms.util.CustomerUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareAccessController {
    private final MinioService minioService;
    private final ShareAccessLogService shareAccessLogService;
    private final ShareAccessService shareAccessService;

    @GetMapping("/view-folder")
    @Operation(summary = "通过分享 Token 访问文件夹及内容")
    public ApiResponse<SharedFolderView> viewSharedFolder(@RequestParam String token) {
        Folder folder = shareAccessService.getFolderByToken(token);
        List<Folder> children = shareAccessService.listChildFolders(folder);
        SharedFolderView result = new SharedFolderView(folder.getName(), folder.getId(), children, folder.getLibraryCode());
        return ApiResponse.success("访问成功", result);
    }

    public record SharedFolderView(String folderName, Long folderId, List<Folder> children, String libraryCode) {
    }

    @GetMapping("/files")
    @Operation(summary = "列出分享目录下的文件列表")
    public ApiResponse<List<UserFile>> listSharedFiles(@RequestParam String token) {
        Folder folder = shareAccessService.getFolderByToken(token);
        List<UserFile> files = shareAccessService.listFilesByFolder(folder);
        return ApiResponse.success("查询成功", files);
    }

    // === 替换原有 /download 方法为“智能下载” ===
    @GetMapping("/download")
    @Operation(summary = "通过分享链接下载（自动判断文件/目录）")
    public ApiResponse<Map<String, String>> downloadFileByShare(
            @Parameter(description = "分享链接 token", required = true, example = "29a20264020147dfa917b86ba1847240")
            @RequestParam String token,

            @Parameter(description = "当 token 指向目录分享时，必须提供目录内目标文件ID；若为文件分享可不传")
            @RequestParam(required = false) Long fileId,
            HttpServletRequest request
    ) {
        ShareAccess share = shareAccessService.getByToken(token);
        if (share == null || !share.getEnabled()) {
            throw new ApiException(403, "分享链接无效或已失效");
        }

        String t = normType(share.getTargetType());
        UserFile file;

        switch (t) {
            case "file" -> {
                // 文件分享：不需要 fileId（若传了则校验一致性）
                file = shareAccessService.getFileByToken(token);
                if (file == null) throw new ApiException(404, "文件不存在或已被删除");
                if (fileId != null && !Objects.equals(file.getId(), fileId)) {
                    throw new ApiException(400, "该分享为单文件，无需 fileId，或 fileId 与分享文件不一致");
                }
            }
            case "folder" -> {
                // 目录分享：必须提供 fileId 且需校验属于该目录
                if (fileId == null) throw new ApiException(400, "该分享为目录，必须提供 fileId");
                Folder folder = shareAccessService.getFolderByToken(token);
                file = shareAccessService.getFileByIdAndValidate(folder, fileId);
                if (file == null) {
                    throw new ApiException(403, "该文件不属于当前分享目录");
                }
            }
            default -> throw new ApiException(400, "不支持的分享类型: " + share.getTargetType());
        }

        // 记录访问日志
        logAccess(request, token, file.getId(), file.getOriginFilename());

        // 生成预签名下载链接（注意使用文件记录中的真实 bucket/key）
        String downloadUrl = minioService.getPresignedDownloadUrl(
                file.getBucket(),               // 真实桶名
                file.getUrl(),                  // 真实对象Key（你系统里命名为 url/ name）
                file.getOriginFilename()        // 用于Content-Disposition的原始文件名
        );

        return ApiResponse.success("获取下载链接成功", Map.of(
                "downloadUrl", downloadUrl,
                "filename", file.getOriginFilename()
        ));
    }

    @PostMapping("/create")
    @Operation(
            summary = "统一创建分享链接",
            description = "根据文件或目录生成分享链接，用户信息由JWT获取，无需前端传入userId和libraryCode"
    )
    @PreAuthorize("hasAnyRole('READER','LIBRARIAN','ADMIN')")
    public ApiResponse<Map<String, String>> createShare(
            @RequestBody @Valid ShareCreateReqVO reqVO
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails)) {
            throw new ApiException(401, "用户未登录");
        }
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        LocalDateTime expireAt = reqVO.getExpireAt();
        if (expireAt == null) {
            expireAt = LocalDateTime.now().plusDays(7);
        }

        String token = shareAccessService.createShare(
                userId,
                reqVO.getType(),
                reqVO.getTargetId(),
                java.sql.Timestamp.valueOf(expireAt),
                libraryCode
        );

        // 返回 token + type
        return ApiResponse.success(Map.of(
                "token", token,
                "type", normType(reqVO.getType())
        ));
    }


    // === 在类中增加一个小工具方法（私有） ===
    private static String normType(String type) {
        return type == null ? "" : type.trim().toLowerCase();
    }




    @PostMapping("/revoke")
    @Operation(summary = "撤销分享链接")
    public ResponseEntity<?> revokeShare(@RequestParam Long userId, @RequestParam String token) {
        shareAccessService.revokeShare(userId, token);
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
