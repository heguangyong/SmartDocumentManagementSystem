package com.github.sdms.util;

import com.github.sdms.dto.UserInfo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 用于缓存通过传递参数uid，调用接口获取用户信息并缓存。
 * 设定定期（6小时）过期失效策略
 */
public class UserInfoCache {

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long EXPIRE_MILLIS = 6 * 60 * 60 * 1000L; // 6小时

    private static class CacheEntry {
        final UserInfo userInfo;
        final long timestamp;

        CacheEntry(UserInfo userInfo) {
            this.userInfo = userInfo;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_MILLIS;
        }
    }

    public static void put(UserInfo userInfo) {
        if (userInfo == null || userInfo.uid == null) return;
        CACHE.put(userInfo.uid, new CacheEntry(userInfo));
    }

    public static UserInfo get(String uid) {
        CacheEntry entry = CACHE.get(uid);
        if (entry == null) return null;
        return entry.isExpired() ? null : entry.userInfo;
    }

    public static List<UserInfo> listAll() {
        return CACHE.values().stream()
                .filter(e -> !e.isExpired())
                .map(e -> e.userInfo)
                .collect(Collectors.toList());
    }

    public static void refresh(String uid, Supplier<UserInfo> fetcher) {
        try {
            UserInfo userInfo = fetcher.get();
            if (userInfo != null) put(userInfo);
        } catch (Exception e) {
            // 可加入日志
        }
    }

    public static Set<String> allKeys() {
        return CACHE.keySet();
    }

    public static void clear() {
        CACHE.clear();
    }
}

