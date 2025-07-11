package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.service.MinioClientService;
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

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioClientService minioClientService;

    @Operation(summary = "上传文件（当前用户）【权限：馆员及管理员】")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")  // 读者无上传权限
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

    @Operation(summary = "管理员上传文件（指定 uid）【权限：仅管理员】")
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

    @Operation(summary = "下载链接生成，含权限校验【权限：读者及以上】")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")  // 所有登录用户可下载
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<String>> downloadFile(
            @RequestParam("filename") String objectName,
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
            String downloadUrl = minioClientService.generatePresignedDownloadUrl(uid, objectName);
            return ResponseEntity.ok(ApiResponse.success(downloadUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.failure("生成下载链接失败: " + e.getMessage()));
        }
    }

}
