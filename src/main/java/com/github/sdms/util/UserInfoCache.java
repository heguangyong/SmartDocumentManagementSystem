package com.github.sdms.util;

import com.github.sdms.dto.UserInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 带最大容量与LRU淘汰策略的用户信息缓存类
 */
public class UserInfoCache {

    private static final int MAX_CACHE_SIZE = 2000;
    private static final long EXPIRE_MILLIS = 6 * 60 * 60 * 1000L; // 6小时

    private static final Map<String, CacheEntry> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    });

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
        synchronized (CACHE) {
            CACHE.put(userInfo.uid, new CacheEntry(userInfo));
        }
    }

    public static UserInfo get(String uid) {
        synchronized (CACHE) {
            CacheEntry entry = CACHE.get(uid);
            if (entry == null || entry.isExpired()) {
                CACHE.remove(uid);
                return null;
            }
            return entry.userInfo;
        }
    }

    public static List<UserInfo> listAll() {
        synchronized (CACHE) {
            return CACHE.values().stream()
                    .filter(e -> !e.isExpired())
                    .map(e -> e.userInfo)
                    .collect(Collectors.toList());
        }
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
        synchronized (CACHE) {
            return new HashSet<>(CACHE.keySet());
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
