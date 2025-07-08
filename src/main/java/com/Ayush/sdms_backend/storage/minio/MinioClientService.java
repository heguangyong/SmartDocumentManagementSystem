package com.Ayush.sdms_backend.storage.minio;

import java.util.Map;

public interface MinioClientService {
    String urltoken(Map<String, Object> params);
    String logintimecheck(String uid, String path);
    void loginset(String uid);
}
