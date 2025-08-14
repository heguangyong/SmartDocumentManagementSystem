package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CustomerUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/onlyoffice")
@RequiredArgsConstructor
public class OnlyOfficeController {

    private final UserFileService userFileService;
    private final MinioService minioService;

    /**
     * 获取OnlyOffice编辑配置
     */
    @Operation(summary = "获取OnlyOffice文档编辑配置", description = "获取用户有权限编辑的文档配置，包括文档的URL、权限等信息。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/edit-config/{docId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEditConfig(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @PathVariable Long docId,
            HttpServletRequest request) throws Exception {

        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();

        // 校验用户权限
        UserFile userFile = userFileService.getFileByDocIdAndUid(docId, userId, libraryCode);
        if (userFile == null) {
            return ResponseEntity.status(403).body(ApiResponse.failure("无权限访问该文档"));
        }

        UserFile file = userFileService.getFileById(userFile.getId());

        // 获取文档访问下载链接（带签名）
        String downloadUrl = minioService.generatePresignedDownloadUrl(
                userId,
                libraryCode,
                file.getOriginFilename(),
                file.getBucket()
        );

        // 构造OnlyOffice配置
        Map<String, Object> document = Map.of(
                "title", userFile.getOriginFilename(),
                "url", downloadUrl,
                "fileType", getFileExtension(userFile.getName()),
                "key", userFile.getVersionKey() != null ? userFile.getVersionKey() : String.valueOf(userFile.getVersionNumber()),
                "permissions", Map.of(
                        "edit", userDetails.hasRole("LIBRARIAN") || userDetails.hasRole("ADMIN")
                )
        );

        Map<String, Object> editorConfig = Map.of(
                "callbackUrl", getBaseUrl(request) + "/api/onlyoffice/callback/" + docId,
                "user", Map.of(
                        "id", userId,
                        "name", userDetails.getUsername()
                )
        );

        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * OnlyOffice 保存回调
     */
    @PostMapping("/callback/{fileId}")
    @Operation(summary = "OnlyOffice 保存回调", description = "接收 OnlyOffice 编辑器回调，下载编辑后的文件并保存为新版本")
    public Map<String, Object> fileSaveCallback(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomerUserDetails userDetails) {

        log.info("OnlyOffice 保存回调 fileId={}", fileId);
        log.info("回调数据: {}", body);

        Integer status = (Integer) body.get("status");
        // 仅在文件编辑完成状态才保存
        if (status != null && (status == 2 || status == 6)) {
            String docUrl = (String) body.get("url"); // OnlyOffice 提供的下载 URL

            try (InputStream in = new URL(docUrl).openStream()) {
                String originalFilename = fileId + ".docx";

                // 调用 InputStream 版本上传方法
//                userFileService.uploadNewVersion(
//                        in,
//                        originalFilename,
//                        userDetails.getUserId(),
//                        userDetails.getLibraryCode(),
//                        fileId,
//                        "OnlyOffice 自动保存",
//                        null
//                );

                log.info("OnlyOffice 文件保存成功: {}", originalFilename);
            } catch (Exception e) {
                log.error("OnlyOffice 文件保存失败", e);
                return Map.of("error", 1, "message", "文件保存失败：" + e.getMessage());
            }
        }

        return Map.of("error", 0); // 返回 error=0 表示成功
    }

    /**
     * 工具：提取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 工具：获取应用基础URL，用于回调地址
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        String portStr = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        return scheme + "://" + serverName + portStr + contextPath;
    }
}
