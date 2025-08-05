package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.repository.BucketRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.PermissionValidator;
import com.github.sdms.util.AuthUtils;
import com.github.sdms.util.BucketStatCache;
import com.github.sdms.util.BucketUtil;
import com.github.sdms.util.FileUtil;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
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

    private static final String SECRET_KEY = "12345678"; // 可配置

    private final MinioClient minioClient;
    private final BucketRepository bucketRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PermissionValidator permissionValidator;

    public MinioServiceImpl(MinioClient minioClient, BucketRepository bucketRepository) {
        this.minioClient = minioClient;
        this.bucketRepository = bucketRepository;
    }

    @Override
    public String urltoken(Map<String, Object> params) {
        if (true) return "ok!";

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

        if (sb.length() > 0) sb.setLength(sb.length() - 1);
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
            throw new ApiException("生成校验哈希失败");
        }
    }

    @Override
    public String logintimecheck(String uid, String libraryCode, String path) {
        Set<String> whitelist = Set.of(
                "/api/user/auth",
                "/api/userFile/downloadStatus",
                "/api/files/upload"
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
    public String uploadFile(String bucketName, String uid, MultipartFile file) {
        try {
            // 检查桶是否存在
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created bucket: {}", bucketName);
            }

            String objectName = FileUtil.generateObjectName(file.getOriginalFilename());
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
                log.info("User {} uploaded file to bucket {}: {}", uid, bucketName, objectName);
                return objectName;
            }
        } catch (Exception e) {
            log.error("MinIO 文件上传失败", e);
            throw new ApiException("文件上传失败: " + e.getMessage());
        }
    }


    @Override
    public String uploadFile(String uid, MultipartFile file, String libraryCode) {
        String bucketName = BucketUtil.getBucketName(uid, libraryCode);

        if (!permissionValidator.canWriteBucket(uid, bucketName)) {
            throw new ApiException("无权限上传至桶：" + bucketName);
        }

        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created bucket: {}", bucketName);
            }

            String objectName = FileUtil.generateObjectName(file.getOriginalFilename());
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
                log.info("User {} uploaded file to bucket {}: {}", uid, bucketName, objectName);
                return objectName;
            }
        } catch (Exception e) {
            log.error("MinIO 文件上传失败", e);
            throw new ApiException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String uid, String libraryCode, String objectName) {
        String bucketName = BucketUtil.getBucketName(uid, libraryCode);

        if (!permissionValidator.canReadBucket(uid, bucketName)) {
            throw new ApiException("无权限访问桶：" + bucketName);
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(60 * 5)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成下载链接失败", e);
            throw new ApiException("生成下载链接失败: " + e.getMessage());
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
                            .expiry(60 * 10)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取 presigned url 失败", e);
            throw new ApiException("获取临时访问地址失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectName) {
        if (!permissionValidator.canReadBucket(AuthUtils.getUid(), bucket)) {
            throw new ApiException("无权限访问桶：" + bucket);
        }

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 获取对象失败", e);
            throw new ApiException("获取对象失败: " + objectName);
        }
    }


    @Override
    public void deleteObject(String bucketName, String objectName) {
        if (!permissionValidator.canWriteBucket(AuthUtils.getUid(), bucketName)) {
            throw new ApiException("无权限删除桶中的文件：" + bucketName);
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 删除对象失败", e);
            throw new ApiException("删除对象失败: " + objectName);
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
                            .expiry(60 * 10)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成下载链接失败", e);
            throw new ApiException("生成预签名地址失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFileFromUrl(String uid, String libraryCode, Long docId, String fileUrl) {
        String bucketName = BucketUtil.getBucketName(uid, libraryCode);

        if (!permissionValidator.canWriteBucket(uid, bucketName)) {
            throw new ApiException("无权限上传至桶：" + bucketName);
        }

        try (InputStream in = new URL(fileUrl).openStream()) {
            String objectName = System.currentTimeMillis() + "_onlyoffice_update";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(in, -1, 10485760)
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            log.error("OnlyOffice 文件保存失败", e);
            throw new ApiException("OnlyOffice 文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public Long getBucketIdForUpload(String uid, String libraryCode) {
        // 只处理桶归属，不处理权限
        return bucketRepository
                .findFirstByOwnerUidAndLibraryCode(uid, libraryCode)
                .map(Bucket::getId)
                .orElseThrow(() -> new ApiException("用户无桶可上传，未找到归属桶"));
    }

    @Override
    public long calculateUsedCapacity(String bucketName) {
        try {
            long totalSize = 0L;

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                totalSize += item.size();
            }

            return totalSize;
        } catch (Exception e) {
            log.error("统计桶 {} 使用容量失败", bucketName, e);
            throw new ApiException("统计桶容量失败: " + e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void refreshBucketStatAsync(String bucketName) {
        try {
            long totalSize = 0L;
            int fileCount = 0;

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                totalSize += item.size();
                fileCount++;
            }

            BucketStatCache.put(bucketName, totalSize, fileCount);
        } catch (Exception e) {
            log.warn("异步统计桶容量失败: {}", bucketName, e);
        }
    }

    @Override
    public long getUsedCapacityWithCache(String bucketName) {
        return BucketStatCache.get(bucketName)
                .map(BucketStatCache.CacheEntry::getSizeInBytes)
                .orElseGet(() -> {
                    refreshBucketStatAsync(bucketName);
                    return -1L;
                });

    }


}
