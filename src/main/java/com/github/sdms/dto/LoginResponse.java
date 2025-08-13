package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * 登录响应对象
 * <p>登录成功后返回给客户端的认证信息</p>
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private String token;

    @Schema(description = "令牌类型，固定为 Bearer", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "用户角色列表", example = "[\"ADMIN\", \"LIBRARIAN\"]")
    private List<String> roles;

    @Schema(description = "JWT 签发者（主角色），供 Kong JWT 插件使用", example = "ADMIN")
    private String iss;
}

