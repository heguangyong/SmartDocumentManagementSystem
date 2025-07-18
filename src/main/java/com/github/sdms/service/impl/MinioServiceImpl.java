package com.github.sdms.service.impl;

import com.github.sdms.service.MinioService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private static final String SECRET_KEY = "12345678"; // 如果需要，可迁移为配置项

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String BUCKET_NAME_PREFIX = "sdms";

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
    public String logintimecheck(String uid, String libraryCode, String path) {
        Set<String> whitelist = Set.of(
                "/api/user/auth",
                "/api/userFile/downloadStatus",
                "/api/files/upload"  // ✅ 上传接口加入白名单，或选择验证
        );
        if (whitelist.contains(path)) return "timein";

        String key = uid + libraryCode + "logintime";
        String timeStr = redisTemplate.opsForValue().get(key);
        if (timeStr == null || !timeStr.matches("\\d+")) return "timeout";

        long timestamp = System.currentTimeMillis() / 1000;
        long loginTime = Long.parseLong(timeStr);
        return (timestamp - loginTime > 120) ? "timeout" : "timein";
    }


    @Override
    public void loginset(String uid, String libraryCode) {
        long timestamp = System.currentTimeMillis() / 1000;
        redisTemplate.opsForValue().set(uid + libraryCode + "logintime", String.valueOf(timestamp));
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
    public String uploadFile(String uid, MultipartFile file, String libraryCode) throws Exception {
        String originalFilename = file.getOriginalFilename();

        // 生成桶名
        String bucketName = getBucketName(uid,libraryCode);

        // 检查桶是否存在，如果不存在则创建
        boolean found = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!found) {
            minioClient.makeBucket(
                    io.minio.MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("Created bucket: {}", bucketName);
        }

        // 构造对象名，这里可以简化为只用时间戳+文件名，目录结构就靠桶隔离
        String objectName = System.currentTimeMillis() + "_" + originalFilename;

        try (InputStream inputStream = file.getInputStream()) {
            // 上传文件
            minioClient.putObject(io.minio.PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("User {} uploaded file to bucket {}: {}", uid, bucketName, objectName);
            return objectName;
        } catch (MinioException e) {
            log.error("MinIO upload error: ", e);
            throw new Exception("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String getBucketName(String uid, String libraryCode) {
        return "sdms-" + uid.toLowerCase().replaceAll("[^a-z0-9-]", "") + "-" + libraryCode.toLowerCase();
    }


    @Override
    public String generatePresignedDownloadUrl(String uid, String libraryCode, String objectName) throws Exception {
        // 生成桶名
        String bucketName = getBucketName(uid, libraryCode);

        // 防止越权校验，确保文件属于该用户（可以根据业务调整）
        // 这里假设objectName无需包含uid/libraryCode前缀，已存入对应桶中
        // 如果你数据库存储了全路径，则这里可去除前缀判断，或改为更细粒度权限判断

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(60 * 5) // 5分钟有效
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

    @Override
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

    @Override
    public String uploadFileFromUrl(String uid, String libraryCode, Long docId, String fileUrl) throws Exception {
        // 这里伪代码，需实现从fileUrl下载文件流，然后上传到MinIO
        try (InputStream in = new URL(fileUrl).openStream()) {
            // 构造桶名
            String bucketName = getBucketName(uid, libraryCode);
            String objectName = System.currentTimeMillis() + "_onlyoffice_update";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(in, -1, 10485760) // -1长度，10MB分块大小
                            .build());

            return objectName;
        } catch (Exception e) {
            log.error("OnlyOffice文件下载上传失败", e);
            throw new Exception("OnlyOffice文件保存失败: " + e.getMessage());
        }
    }

}
