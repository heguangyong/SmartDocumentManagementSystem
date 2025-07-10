package com.github.sdms.controller.admin;

import com.github.sdms.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/test")
public class AdminTestController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminPing() {
        return ApiResponse.success("✅ ADMIN 权限验证成功！");
    }
}
