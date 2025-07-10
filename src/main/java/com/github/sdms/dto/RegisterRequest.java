package com.github.sdms.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String username;
    private String email;
    private String password;
    private String role; // 可为 "ROLE_ADMIN" 或 "ROLE_USER"

}
