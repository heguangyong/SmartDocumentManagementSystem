package com.github.sdms.controller;

import com.github.sdms.common.response.ApiResponse;
import com.github.sdms.storage.minio.MinioClientService;
import com.github.sdms.wrapper.CustomerUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioClientService minioClientService;

    @Operation(summary = "上传文件（当前用户）")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            HttpServletRequest request
    ) {
        String uid = userDetails.getUid();
        String path = request.getRequestURI();

        String check = minioClientService.logintimecheck(uid, path);
        if (!"timein".equals(check)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("会话已过期，请重新登录"));
        }

        try {
            String objectName = minioClientService.uploadFile(uid, file);
            return ResponseEntity.ok(ApiResponse.success("文件上传成功: " + objectName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.failure("上传失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "管理员上传文件（指定 uid）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload/admin")
    public ResponseEntity<ApiResponse<String>> uploadFileAsAdmin(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uid") String targetUid
    ) {
        String filename = file.getOriginalFilename();
        String msg = "管理员上传文件成功，文件：" + filename + "，目标用户：" + targetUid;
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    @Operation(summary = "下载链接生成，含权限校验")
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<String>> generateDownloadUrl(
            @RequestParam String filename,
            @RequestParam String uid,
            HttpServletRequest request
    ) {
        // 1. 获取登录用户身份（uid/role）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUsername.equals(uid)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("您无权限下载该用户的文件"));
        }

        // 2. 构造验签参数（可扩展）
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("filename", filename);

        String tokenCheck = minioClientService.urltoken(params);
        if (!"ok!".equals(tokenCheck)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("URL 验签失败"));
        }

        // 3. 登录有效性判断
        String sessionCheck = minioClientService.logintimecheck(uid, request.getRequestURI());
        if (!"timein".equals(sessionCheck)) {
            return ResponseEntity.status(403).body(ApiResponse.failure("登录已过期，请重新登录"));
        }

        // 4. 模拟生成下载链接（后续接入 MinIO 签名下载）
        String downloadUrl = String.format("https://mock-download-url/%s/%s", uid, filename);
        return ResponseEntity.ok(ApiResponse.success(downloadUrl));
    }

}
