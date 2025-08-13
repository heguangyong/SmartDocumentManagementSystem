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
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.sdms.util.FileUtil.convertUrlToMultipartFile;
import static com.github.sdms.util.FileUtil.parseDocIdFromKey;

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

        // 校验用户权限，确保用户有权编辑该文档
        UserFile userFile = userFileService.getFileByDocIdAndUid(docId, userId, libraryCode);
        if (userFile == null) {
            return ResponseEntity.status(403).body(ApiResponse.failure("无权限访问该文档"));
        }
        // 获取文件（内部已做权限校验）
        UserFile file = userFileService.getFileById(userFile.getId());
        // 获取文档访问下载链接（带签名）
        // 生成 MinIO 签名下载链接，使用文件实际桶名
        String downloadUrl = minioService.generatePresignedDownloadUrl(
                userDetails.getUserId(),
                userDetails.getLibraryCode(),
                file.getOriginFilename(),
                file.getBucket()
        );
        // 构造OnlyOffice编辑器配置
        Map<String, Object> config = new HashMap<>();
        config.put("document", Map.of(
                "title", userFile.getOriginFilename(),
                "url", downloadUrl,
                "fileType", getFileExtension(userFile.getName()),
                "key", userFile.getVersionKey() != null ? userFile.getVersionKey() : String.valueOf(userFile.getVersionNumber()),
                "permissions", Map.of(
                        "edit", userDetails.hasRole("LIBRARIAN") || userDetails.hasRole("ADMIN")  // 只馆员及管理员可编辑
                )
        ));
        config.put("editorConfig", Map.of(
                "callbackUrl", getBaseUrl(request) + "/api/onlyoffice/onlyofficeCallback",
                "user", Map.of(
                        "id", userId,
                        "name", userDetails.getUsername()
                )
        ));

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @Operation(summary = "OnlyOffice文档编辑保存回调", description = "接收OnlyOffice编辑器的回调，保存文档的新版本。")
    @PostMapping("/onlyofficeCallback")
    public ResponseEntity<String> onlyofficeCallback(@RequestBody Map<String, Object> callbackData,
                                                     @AuthenticationPrincipal CustomerUserDetails userDetails,
                                                     @RequestParam(required = false) Long folderId) {
        log.info("OnlyOffice Callback data: {}", callbackData);

        Integer status = (Integer) callbackData.get("status");
        if (status != null && (status == 2 || status == 6)) {
            String docUrl = (String) callbackData.get("url");
            String key = (String) callbackData.get("key");
            Long docId = parseDocIdFromKey(key); // 自定义方法，根据版本key找到docId

            Long userId = userDetails.getUserId();
            String libraryCode = userDetails.getLibraryCode();

            try {
                // 先通过下载URL保存到MinIO，再保存新版本
                String objectName = minioService.uploadFileFromUrl(userId, libraryCode, docId, docUrl);

                // 调用UserFileService上传新版本
                MultipartFile multipartFile = convertUrlToMultipartFile(docUrl); // 需实现转换方法，或者调整逻辑
                userFileService.uploadNewVersion(multipartFile, userId, libraryCode, docId, "OnlyOffice自动保存", folderId);

                log.info("OnlyOffice文档保存成功，新对象名：{}", objectName);
            } catch (Exception e) {
                log.error("OnlyOffice文件保存失败", e);
            }
        }
        return ResponseEntity.ok("{\"error\":0}");
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
        String scheme = request.getScheme();             // http
        String serverName = request.getServerName();     // localhost
        int serverPort = request.getServerPort();        // 8080
        String contextPath = request.getContextPath();   // /api

        // 端口 80 或 443 不显示
        String portStr = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        return scheme + "://" + serverName + portStr + contextPath;
    }

    /**
     * 获取 OnlyOffice 编辑配置
     */
    @GetMapping("/config/{fileId}")
    public Map<String, Object> getOnlyOfficeConfig(@PathVariable Long fileId,
                                                   @AuthenticationPrincipal CustomerUserDetails userDetails) {
        String fileName = fileId + ".docx";

        // 生成 MinIO 预签名下载链接
        String fileUrl = minioService.generatePresignedDownloadUrl(
                userDetails.getUserId(),
                userDetails.getLibraryCode(),
                fileName,
                "bucket001"  // 你的桶名
        );

        String key = UUID.randomUUID().toString();

        Map<String, Object> document = Map.of(
                "fileType", "docx",
                "key", key,
                "title", fileName,
                "url", fileUrl
        );

        Map<String, Object> editorConfig = Map.of(
                "callbackUrl", "http://192.168.1.198:8080/api/onlyoffice/callback/" + fileId,
                "user", Map.of(
                        "id", userDetails.getUserId(),
                        "name", userDetails.getUsername()
                )
        );

        return Map.of(
                "documentType", "word",
                "document", document,
                "editorConfig", editorConfig
        );
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

        System.out.println("OnlyOffice 保存回调 fileId=" + fileId);
        System.out.println("回调数据: " + body);

        Integer status = (Integer) body.get("status");
        // 仅在文件编辑完成状态才保存
        if (status != null && (status == 2 || status == 6)) {
            String docUrl = (String) body.get("url"); // OnlyOffice 提供的下载 URL
            String key = (String) body.get("key");   // OnlyOffice 版本 key，可用作版本标识

            Long userId = userDetails.getUserId();
            String libraryCode = userDetails.getLibraryCode();

            try (InputStream in = new URL(docUrl).openStream()) {
                // 获取文件原名，可用 docId 查数据库或默认 fileId.docx
                String originalFilename = fileId + ".docx";

                // 调用 uploadNewVersion(InputStream...) 保存新版本
//                userFileService.uploadNewVersionByInputStream(in, originalFilename, userId, libraryCode,
//                        fileId, "OnlyOffice 自动保存", null);

                System.out.println("OnlyOffice 文件保存成功: " + originalFilename);
            } catch (Exception e) {
                e.printStackTrace();
                return Map.of("error", 1, "message", "文件保存失败：" + e.getMessage());
            }
        }

        return Map.of("error", 0); // 返回 error=0 表示成功
    }


}
