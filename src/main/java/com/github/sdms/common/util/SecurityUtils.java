package com.github.sdms.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文信息获取工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 UID（通常为 email 或 OAuth uid）
     */
    public static String getCurrentUid() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // username 实际是 uid 或 email
        }
        return null;
    }

    /**
     * 获取当前登录用户的角色（取第一个角色字符串）
     */
    public static String getCurrentRole() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getAuthorities().stream()
                    .map(Object::toString)
                    .findFirst()
                    .orElse("USER");
        }
        return "GUEST";
    }

    /**
     * 获取当前认证对象（可判断是否已登录）
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断是否已登录（不为匿名用户）
     */
    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
