package com.github.sdms.controller.admin;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private MinioService minioService;
    @Resource
    private ShareAccessLogService shareAccessLogService;


    @Operation(summary = "清理上传缓存（仅管理员权限）")
    @PostMapping("/clear-upload-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearUploadCache() {
        boolean result = minioService.clearUploadCache();
        return ResponseEntity.ok(ApiResponse.success(result ? "清理成功 ✅" : "清理失败 ❌"));
    }

    @Operation(summary = "管理员权限测试接口（仅管理员权限）")
    @GetMapping("/test/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminPing() {
        return ResponseEntity.ok(ApiResponse.success("✅ ADMIN 权限验证成功！"));
    }

    @Operation(summary = "查询所有分享访问日志（仅管理员）")
    @GetMapping("/share-access-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getShareAccessLogs() {
        return ResponseEntity.ok(ApiResponse.success("查询成功", shareAccessLogService.getAllLogs()));
    }

}
