package com.github.sdms.util;

public class PasswordUtil {

    /**
     * 校验密码强度
     * 规则：至少8位，包含大小写字母、数字和特殊字符
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        // 至少8位，包含小写、大写、数字、特殊字符
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-=]).{8,}$";
        return password.matches(pattern);
    }
}
