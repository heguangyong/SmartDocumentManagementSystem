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
            return userDetails.getUsername(); // 退回到username
        } else if (principal instanceof String) {
            return (String) principal;
        } else {
            throw new RuntimeException("无法获取用户身份信息");
        }
    }

    /**
     * 获取当前登录用户的 UID（通常为 username 或 OAuth uid）
     */
    public static String getCurrentUid() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // username 实际是 uid
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


    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomerUserDetails userDetails) {
            return userDetails.getUserId(); // 新增方法，返回Long类型用户ID
        } else if (principal instanceof UserDetails) {
            // 如果UserDetails中无userId，抛异常或返回null，根据实际实现
            throw new RuntimeException("无法获取用户ID");
        } else if (principal instanceof String) {
            // 无法获取ID，抛异常或返回null
            throw new RuntimeException("无法获取用户ID");
        } else {
            throw new RuntimeException("无法获取用户身份信息");
        }
    }

}

