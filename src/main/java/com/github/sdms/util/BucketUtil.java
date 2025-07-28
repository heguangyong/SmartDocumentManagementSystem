package com.github.sdms.util;


public class BucketUtil {
    private static final String BUCKET_NAME_PREFIX = "sdms";

    public static String getBucketName(String uid, String libraryCode) {
        return BUCKET_NAME_PREFIX + "-" + uid.toLowerCase().replaceAll("[^a-z0-9-]", "") + "-" + libraryCode.toLowerCase();
    }
}
