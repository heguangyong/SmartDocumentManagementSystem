package com.github.sdms.util;

public class BucketUtil {
    private static final String BUCKET_NAME_PREFIX = "sdms";

    public static String getBucketName(Long ownerId, String libraryCode) {
        // ownerId 转字符串
        String ownerIdStr = ownerId.toString();
        // 这里不需要替换特殊字符，因为 ownerId 是数字字符串
        return BUCKET_NAME_PREFIX + "-" + ownerIdStr + "-" + libraryCode.toLowerCase();
    }
}
