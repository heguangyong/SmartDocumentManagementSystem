package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.MinioService;
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

    private final MinioService minioService;
    private final UserRepository userRepository;

    /**
     * 上传文件（当前用户）
     * 权限：馆员及管理员
     */
    @Operation(summary = "上传文件（当前用户）【权限：馆员及管理员】")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")  // 读者无上传权限
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            HttpServletRequest request
    ) {
        String uid = userDetails.getUid();
        String libraryCode = userDetails.getLibraryCode();  // 获取 libraryCode
        String path = request.getRequestURI();

        // 使用 uid 和 libraryCode 验证会话
        String check = minioService.logintimecheck(uid, libraryCode, path);
        if (!"timein".equals(check)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("会话已过期，请重新登录"));
        }

        try {
            String objectName = minioService.uploadFile(uid, file, libraryCode);
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
            @RequestParam("uid") String targetUid,
            @RequestParam("libraryCode") String libraryCode  // 确保管理员提供 libraryCode
    ) {
        String filename = file.getOriginalFilename();

        // 校验目标用户是否存在于指定馆
        AppUser targetUser = userRepository.findByUidAndLibraryCode(targetUid, libraryCode).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(ApiResponse.failure("用户不存在或不属于指定馆"));
        }

        try {
            // 实际上传
            String objectName = minioService.uploadFile(targetUid, file, libraryCode);

            String msg = String.format("✅ 管理员上传成功 - 用户: %s | 馆: %s | 文件: %s", targetUid, libraryCode, objectName);
            return ResponseEntity.ok(ApiResponse.success(msg));
        } catch (Exception e) {
            String err = String.format("❌ 管理员上传失败 - 用户: %s | 文件: %s | 错误: %s", targetUid, filename, e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.failure(err));
        }
    }


    /**
     * 下载文件并生成下载链接
     * 权限：读者及以上
     */
    @Operation(summary = "下载链接生成，含权限校验【权限：读者及以上】")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")  // 所有登录用户可下载
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<String>> downloadFile(
            @RequestParam("filename") String objectName,
            @AuthenticationPrincipal CustomerUserDetails userDetails,
            HttpServletRequest request
    ) {
        String uid = userDetails.getUid();
        String libraryCode = userDetails.getLibraryCode();  // 获取 libraryCode
        String path = request.getRequestURI();

        // 使用 uid 和 libraryCode 验证会话
        String check = minioService.logintimecheck(uid, libraryCode, path);
        if (!"timein".equals(check)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("会话已过期，请重新登录"));
        }

        try {
            String downloadUrl = minioService.generatePresignedDownloadUrl(uid, libraryCode, objectName);
            return ResponseEntity.ok(ApiResponse.success(downloadUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.failure("生成下载链接失败: " + e.getMessage()));
        }
    }

}
