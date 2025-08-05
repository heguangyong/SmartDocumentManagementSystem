package com.github.sdms.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 桶统计缓存（容量与文件数），LRU + 过期控制
 */
public class BucketStatCache {

    private static final int MAX_CACHE_SIZE = 2000;
    private static final long EXPIRE_MILLIS = 6 * 60 * 60 * 1000L; // 6小时

    private static final Map<String, CacheEntry> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    });

    public static class CacheEntry {
        private final long sizeInBytes;
        private final int fileCount;
        private final long timestamp;

        public CacheEntry(long sizeInBytes, int fileCount) {
            this.sizeInBytes = sizeInBytes;
            this.fileCount = fileCount;
            this.timestamp = System.currentTimeMillis();
        }

        public long getSizeInBytes() {
            return sizeInBytes;
        }

        public int getFileCount() {
            return fileCount;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_MILLIS;
        }
    }

    public static void put(String bucketName, long sizeInBytes, int fileCount) {
        if (bucketName == null) return;
        synchronized (CACHE) {
            CACHE.put(bucketName, new CacheEntry(sizeInBytes, fileCount));
        }
    }

    public static Optional<CacheEntry> get(String bucketName) {
        synchronized (CACHE) {
            CacheEntry entry = CACHE.get(bucketName);
            if (entry == null || entry.isExpired()) {
                CACHE.remove(bucketName);
                return Optional.empty();
            }
            return Optional.of(entry);
        }
    }

    public static List<String> listBuckets() {
        synchronized (CACHE) {
            return CACHE.keySet().stream().collect(Collectors.toList());
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
