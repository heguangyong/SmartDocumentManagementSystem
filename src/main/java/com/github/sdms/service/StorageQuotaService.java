package com.github.sdms.service;

public interface StorageQuotaService {
    /**
     * 获取指定用户最大配额（字节）
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 最大存储配额
     */
    long getMaxQuota(String uid, String libraryCode);

    /**
     * 获取指定用户当前已用配额（字节）
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 已用存储配额
     */
    long getUsedQuota(String uid, String libraryCode);

    /**
     * 判断是否可上传指定大小文件
     * @param uid 用户ID
     * @param fileSize 文件大小（字节）
     * @param libraryCode 租户标识
     * @return 如果可上传，返回 true，否则返回 false
     */
    boolean canUpload(String uid, long fileSize, String libraryCode);

    /**
     * 获取剩余配额
     * @param uid 用户ID
     * @param libraryCode 租户标识
     * @return 剩余存储配额
     */
    long getRemainingQuota(String uid, String libraryCode);
}
