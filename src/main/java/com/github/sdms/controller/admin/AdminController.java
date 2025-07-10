package com.github.sdms.controller.admin;

import com.github.sdms.common.response.ApiResponse;
import com.github.sdms.storage.minio.MinioClientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private MinioClientService minioClientService;

    @Operation(summary = "清理上传缓存（仅管理员）")
    @PostMapping("/clear-upload-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearUploadCache() {
        boolean result = minioClientService.clearUploadCache(); // 你可以自定义这个方法
        return ResponseEntity.ok(ApiResponse.success(result ? "清理成功 ✅" : "清理失败 ❌"));
    }
}
