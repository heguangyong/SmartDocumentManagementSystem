package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.PermissionValidator;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CustomerUserDetails;
import com.github.sdms.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/onlyoffice")
@RequiredArgsConstructor
public class OnlyOfficeController {

    private final UserFileService userFileService;
    private final MinioService minioService;
    private final JwtUtil jwtUtil;

    @Autowired
    private PermissionValidator permissionValidator;

    @Value("${onlyoffice.server.url:http://localhost:8081}")
    private String onlyOfficeServerUrl;

    @Value("${app.external-base-url}")
    private String externalBaseUrl;

    /**
     * 获取OnlyOffice编辑配置
     */
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/edit-config/{fileId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEditConfig(
            @PathVariable Long fileId,
            HttpServletRequest request) throws Exception {

        CustomerUserDetails userDetails = (CustomerUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserFile file = userFileService.getFileById(fileId);
        Long userId = userDetails.getUserId();

        if (file == null) {
            return ResponseEntity.status(403).body(ApiResponse.failure("无权限访问该文档"));
        }

        // 生成长期有效的token（用于OnlyOffice下载文档）
        String documentToken = jwtUtil.generateDocumentToken(userDetails, fileId);
        log.info("生成文档 token: {}, fileId: {}", documentToken, fileId);
        // 关键修复：使用externalBaseUrl确保OnlyOffice容器能访问
        String downloadUrl = externalBaseUrl + "/api/file/download-proxy/" + file.getId() + "?token=" + documentToken;

        log.info("生成文档下载URL: {}", downloadUrl);

        // 检查用户写权限
        boolean canEdit = permissionValidator.canWriteFile(userId, file.getId()) &&
                (userDetails.hasRole("LIBRARIAN") || userDetails.hasRole("ADMIN"));

        log.info("用户权限检查 - userId: {}, fileId: {}, canEdit: {}", userId, file.getId(), canEdit);

        // 构造OnlyOffice配置
        Map<String, Object> document = Map.of(
                "title", file.getOriginFilename(),
                "url", downloadUrl,
                "fileType", getFileExtension(file.getOriginFilename()),
                "key", generateDocumentKey(file), // 使用确定性的key生成
                "permissions", Map.of(
                        "edit", canEdit,
                        "download", true,
                        "print", true,
                        "review", canEdit,
                        "comment", canEdit,
                        "fillForms", canEdit,
                        "modifyFilter", canEdit,
                        "modifyContentControl", canEdit
                )
        );

        Map<String, Object> editorConfig = Map.of(
                "callbackUrl", externalBaseUrl + "/api/onlyoffice/callback/" + fileId,
                "user", Map.of(
                        "id", String.valueOf(userId),
                        "name", userDetails.getUsername()
                ),
                "lang", "zh-CN",
                "mode", canEdit ? "edit" : "view"
        );

        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        config.put("documentType", getDocumentType(getFileExtension(file.getOriginFilename())));
        config.put("type", "desktop");
        config.put("width", "100%");
        config.put("height", "100%");

        log.info("OnlyOffice配置生成成功 - 文档ID: {}, 下载URL: {}, 编辑模式: {}",
                fileId, downloadUrl, canEdit ? "编辑" : "只读");

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 生成确定性的文档key
     */
    private String generateDocumentKey(UserFile file) {
        // 使用文件ID + 版本号 + 文件修改时间生成稳定的key
        long timestamp = file.getUpdateTime() != null ? file.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : file.getCreatedDate().getTime();
        return String.format("doc_%d_v%d_%d",
                file.getId(),
                file.getVersionNumber() != null ? file.getVersionNumber() : 1,
                timestamp / 1000 // 秒级时间戳，避免毫秒变化
        );
    }

    /**
     * 测试文档下载连通性的接口
     */
    @GetMapping("/test-download/{fileId}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> testDownload(@PathVariable Long fileId, HttpServletRequest request) {
        try {
            CustomerUserDetails userDetails = (CustomerUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserFile file = userFileService.getFileById(fileId);

            if (file == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "文件不存在"));
            }

            String documentToken = jwtUtil.generateDocumentToken(userDetails, fileId);
            String downloadUrl = externalBaseUrl + "/api/file/download-proxy/" + file.getId() + "?token=" + documentToken;

            // 测试URL可访问性
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();

                return ResponseEntity.ok(Map.of(
                        "success", responseCode == 200,
                        "responseCode", responseCode,
                        "downloadUrl", downloadUrl,
                        "message", responseCode == 200 ? "下载链接正常" : "下载链接异常"
                ));
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "downloadUrl", downloadUrl,
                        "error", e.getMessage()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * OnlyOffice 保存回调 - 支持匿名访问（OnlyOffice服务器回调）
     */
    @PostMapping("/callback/{fileId}")
    @Operation(summary = "OnlyOffice 保存回调", description = "接收 OnlyOffice 编辑器回调，下载编辑后的文件并保存为新版本")
    public Map<String, Object> fileSaveCallback(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> body) {

        log.info("OnlyOffice 保存回调 fileId={}", fileId);
        log.info("回调数据: {}", body);

        try {
            Integer status = (Integer) body.get("status");

            // OnlyOffice状态说明：
            // 0 - 文档未找到
            // 1 - 文档编辑中
            // 2 - 文档已保存
            // 3 - 文档保存错误
            // 4 - 文档关闭但无更改
            // 6 - 文档编辑中，但当前用户已断开连接
            // 7 - 强制保存错误

            if (status == null) {
                log.warn("回调状态为空");
                return Map.of("error", 1, "message", "状态参数缺失");
            }

            // 状态为2或6时需要保存文档
            if (status == 2 || status == 6) {
                String docUrl = (String) body.get("url");
                if (docUrl == null || docUrl.isEmpty()) {
                    log.error("文档下载URL为空");
                    return Map.of("error", 1, "message", "文档URL缺失");
                }

                // 获取文件信息
                UserFile file = userFileService.getFileById(fileId);
                if (file == null) {
                    log.error("文件不存在: {}", fileId);
                    return Map.of("error", 1, "message", "文件不存在");
                }

                // 从OnlyOffice下载编辑后的文档
                try (InputStream inputStream = new URL(docUrl).openStream()) {

                    // 保存为新版本
                    // 注意：这里需要根据你的实际uploadNewVersion方法签名进行调整
                    userFileService.uploadNewVersion(
                            inputStream,
                            file.getOriginFilename(),
                            file.getUserId(), // 使用原文件的用户ID
                            file.getLibraryCode(), // 使用原文件的图书馆代码
                            fileId,
                            "OnlyOffice在线编辑保存", // 版本说明
                            null // 标签，如果需要的话
                    );

                    log.info("OnlyOffice 文件保存成功: fileId={}, filename={}", fileId, file.getOriginFilename());

                } catch (Exception e) {
                    log.error("下载或保存编辑后的文档失败: fileId=" + fileId, e);
                    return Map.of("error", 1, "message", "文件保存失败：" + e.getMessage());
                }
            }

            // 返回成功响应
            return Map.of("error", 0);

        } catch (Exception e) {
            log.error("处理OnlyOffice回调异常: fileId=" + fileId, e);
            return Map.of("error", 1, "message", "处理回调失败：" + e.getMessage());
        }
    }

    /**
     * 提取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 根据文件扩展名确定文档类型
     */
    private String getDocumentType(String extension) {
        switch (extension.toLowerCase()) {
            case "doc":
            case "docx":
            case "docm":
            case "dot":
            case "dotx":
            case "dotm":
            case "odt":
            case "fodt":
            case "ott":
            case "rtf":
            case "txt":
                return "text";

            case "xls":
            case "xlsx":
            case "xlsm":
            case "xlt":
            case "xltx":
            case "xltm":
            case "ods":
            case "fods":
            case "ots":
            case "csv":
                return "spreadsheet";

            case "ppt":
            case "pptx":
            case "pptm":
            case "pot":
            case "potx":
            case "potm":
            case "odp":
            case "fodp":
            case "otp":
                return "presentation";

            default:
                return "text"; // 默认文本类型
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        return externalBaseUrl;
    }
}