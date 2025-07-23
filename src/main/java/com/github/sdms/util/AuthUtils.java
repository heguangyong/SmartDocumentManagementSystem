package com.github.sdms.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUtils {

    public static String getUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomerUserDetails userDetails) {
            return userDetails.getUid(); // 从自定义 UserDetails 里取真实 uid
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // 退回到username（邮箱）
        } else if (principal instanceof String) {
            return (String) principal;
        } else {
            throw new RuntimeException("无法获取用户身份信息");
        }
    }
}

