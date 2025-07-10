package com.github.sdms.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface MinioClientService {
    String urltoken(Map<String, Object> params);
    String logintimecheck(String uid, String path);
    void loginset(String uid);
    boolean clearUploadCache();
    List<String> getUserFiles(String username);
    // 新增上传方法
    String uploadFile(String uid, MultipartFile file) throws Exception;
    String generatePresignedDownloadUrl(String uid, String objectName) throws Exception;
    String getPresignedUrl(String bucket, String objectName);
    InputStream getObject(String bucket, String objectName);
    void deleteObject(String bucketName, String objectName);
    String getPresignedDownloadUrl(String bucket, String objectKey, String filename);

}
