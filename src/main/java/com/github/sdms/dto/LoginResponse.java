package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private List<String> roles;
    private String iss; // ✅ 主角色（供 Kong JWT 插件使用）
}
