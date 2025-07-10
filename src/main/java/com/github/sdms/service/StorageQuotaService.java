package com.github.sdms.service;

public interface StorageQuotaService {
    /**
     * 获取指定用户最大配额（字节）
     */
    long getMaxQuota(String uid);

    /**
     * 获取指定用户当前已用配额（字节）
     */
    long getUsedQuota(String uid);

    /**
     * 判断是否可上传指定大小文件
     */
    boolean canUpload(String uid, long fileSize);

    /**
     * 获取剩余配额
     */
    long getRemainingQuota(String uid);
}
