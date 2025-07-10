package com.github.sdms.storage.minio;

import java.util.List;
import java.util.Map;

public interface MinioClientService {
    String urltoken(Map<String, Object> params);
    String logintimecheck(String uid, String path);
    void loginset(String uid);
    boolean clearUploadCache();
    List<String> getUserFiles(String username);
}
