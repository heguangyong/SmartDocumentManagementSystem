package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequest {

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "密码", example = "password123")
    private String password;

    @Schema(description = "图书馆代码", example = "shlib")
    private String libraryCode;

    @Schema(description = "图像验证码ID")
    private String captchaId;

    @Schema(description = "图像验证码内容")
    private String captchaCode;

    @Schema(description = "是否记住我", example = "false")
    private boolean rememberMe;
}
