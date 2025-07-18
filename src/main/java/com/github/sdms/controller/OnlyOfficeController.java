package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CustomerUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

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
    @Operation(summary = "获取OnlyOffice编辑配置")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/edit-config/{docId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEditConfig(
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            @PathVariable Long docId,
            HttpServletRequest request) throws Exception {

        String uid = userDetails.getUid();
        String libraryCode = userDetails.getLibraryCode();

        // 校验用户权限，确保用户有权编辑该文档
        UserFile userFile = userFileService.getFileByDocIdAndUid(docId, uid, libraryCode);
        if (userFile == null) {
            return ResponseEntity.status(403).body(ApiResponse.failure("无权限访问该文档"));
        }

        // 获取文档访问下载链接（带签名）
        String downloadUrl = minioService.generatePresignedDownloadUrl(userFile.getUid(), libraryCode, userFile.getName());

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
                        "id", uid,
                        "name", userDetails.getUsername()
                )
        ));

        return ResponseEntity.ok(ApiResponse.success(config));
    }

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

            String uid = userDetails.getUid();
            String libraryCode = userDetails.getLibraryCode();

            try {
                // 先通过下载URL保存到MinIO，再保存新版本
                String objectName = minioService.uploadFileFromUrl(uid, libraryCode, docId, docUrl);

                // 调用UserFileService上传新版本
                MultipartFile multipartFile = convertUrlToMultipartFile(docUrl); // 需实现转换方法，或者调整逻辑
                userFileService.uploadNewVersion(multipartFile, uid, libraryCode, docId, "OnlyOffice自动保存",folderId);

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
}
