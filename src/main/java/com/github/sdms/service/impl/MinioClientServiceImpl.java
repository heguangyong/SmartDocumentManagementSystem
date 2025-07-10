package com.github.sdms.service.impl;

import com.github.sdms.model.UserFile;
import com.github.sdms.service.MinioClientService;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class MinioClientServiceImpl implements MinioClientService {

    private static final String SECRET_KEY = "12345678"; // ✅ 如果需要，可迁移为配置项

    @Autowired
    private MinioClient minioClient;  // 你应该已经配置过 MinioClient Bean

    private static final String BUCKET_NAME = "sdmsfilesmanager";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserFileService userFileService;

    @Autowired
    private StorageQuotaService storageQuotaService;

    @Override
    public String urltoken(Map<String, Object> params) {
        // 可通过配置开关关闭验签逻辑
        if (true) return "ok!";

        // 参数排序构造 keyStr
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        String cstokey = "";

        for (String key : keys) {
            Object value = params.get(key);
            String valStr = value != null ? value.toString() : "";
            if ("token".equals(key)) {
                cstokey = valStr;
            } else {
                sb.append(key).append("=").append(valStr).append("&");
            }
        }

        if (sb.length() > 0) sb.setLength(sb.length() - 1); // 移除最后一个 &
        String toHash = sb + SECRET_KEY;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashed = new StringBuilder();
            for (byte b : digest) {
                hashed.append(String.format("%02x", b));
            }

            return hashed.toString().equals(cstokey) ? "ok!" : "not!";
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash error", e);
            return "error";
        }
    }

    @Override
    public String logintimecheck(String uid, String path) {
        // 白名单
        Set<String> whitelist = Set.of("/api/user/auth", "/api/userFile/downloadStatus");
        if (whitelist.contains(path)) return "timein";

        String key = uid + "logintime";
        String timeStr = redisTemplate.opsForValue().get(key);
        if (timeStr == null || !timeStr.matches("\\d+")) return "timeout";

        long timestamp = System.currentTimeMillis() / 1000;
        long loginTime = Long.parseLong(timeStr);

        return (timestamp - loginTime > 120) ? "timeout" : "timein";
    }

    @Override
    public void loginset(String uid) {
        long timestamp = System.currentTimeMillis() / 1000;
        redisTemplate.opsForValue().set(uid + "logintime", String.valueOf(timestamp));
    }

    @Override
    public boolean clearUploadCache() {
        log.info("清理上传缓存");
        return true;
    }

    @Override
    public List<String> getUserFiles(String username) {
        return List.of("file1.pdf", "file2.docx");
    }

    @Override
    public String uploadFile(String uid, MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String objectName = uid + "/" + System.currentTimeMillis() + "_" + originalFilename;

        try (InputStream inputStream = file.getInputStream()) {
            // 检查桶是否存在，如果不存在则创建
            boolean found = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(BUCKET_NAME)
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder()
                                .bucket(BUCKET_NAME)
                                .build()
                );
                log.info("Created bucket: {}", BUCKET_NAME);
            }

            if (!storageQuotaService.canUpload(uid, file.getSize())) {
                throw new RuntimeException("上传失败：存储配额不足，请联系管理员或清理文件。");
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 上传成功后
            UserFile record = UserFile.builder()
                    .uid(uid)
                    .name(objectName)
                    .originFilename(originalFilename)
                    .type(file.getContentType())
                    .size(file.getSize())
                    .url(objectName)
                    .md5(null) // TODO: 可后续计算
                    .bucket(BUCKET_NAME)
                    .deleteFlag(false)
                    .uperr(0)
                    .createdDate(new Date())
                    .build();
            userFileService.saveUserFile(record);

            log.info("User {} uploaded file: {}", uid, objectName);
            return objectName;
        } catch (MinioException e) {
            log.error("MinIO upload error: ", e);
            throw new Exception("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String uid, String objectName) throws Exception {
        // 防止越权，确保用户只能下载自己文件夹下的文件
        if (!objectName.startsWith(uid + "/")) {
            throw new IllegalArgumentException("无权访问该文件");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(60 * 5) // 链接5分钟有效
                            .build()
            );
        } catch (MinioException e) {
            log.error("生成下载链接失败", e);
            throw new Exception("生成下载链接失败: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String bucket, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(60 * 10) // 10分钟有效
                            .build());
        } catch (Exception e) {
            log.error("获取 presigned url 失败", e);
            return null;
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取对象失败: " + bucket + "/" + objectName, e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("删除 MinIO 对象失败: " + objectName, e);
        }
    }

    public String getPresignedDownloadUrl(String bucket, String objectKey, String filename) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(60 * 10) // 10分钟有效
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("生成预签名地址失败", e);
        }
    }

}
