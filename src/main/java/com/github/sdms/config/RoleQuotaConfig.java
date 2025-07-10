package com.github.sdms.config;

import java.util.Map;

public class RoleQuotaConfig {
    // 单位：字节（1GB, 10GB）
    public static final Map<String, Long> ROLE_QUOTA = Map.of(
            "READER", 1L * 1024 * 1024 * 1024,
            "LIBRARIAN", 10L * 1024 * 1024 * 1024,
            "ADMIN", Long.MAX_VALUE // 无限
    );

    public static long getQuota(String role) {
        return ROLE_QUOTA.getOrDefault(role.toUpperCase(), 0L);
    }
}
