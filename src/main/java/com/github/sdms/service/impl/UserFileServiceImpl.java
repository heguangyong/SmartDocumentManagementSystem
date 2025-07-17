package com.github.sdms.service.impl;

import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CachedIdGenerator;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class UserFileServiceImpl implements UserFileService {
    private static final Logger log = LoggerFactory.getLogger(UserFileServiceImpl.class);

    @Autowired
    private UserFileRepository userFileRepository;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioService minioService;

    @Autowired
    private StorageQuotaService storageQuotaService;

    @Autowired
    private CachedIdGenerator cachedIdGenerator;

    @Value("${file.share.secret}")
    private String shareSecret;

    @Value("${file.share.default-expire-millis}")
    private long defaultExpireMillis ;

    @Autowired
    private ShareAccessLogService shareAccessLogService;

    @Override
    public void saveUserFile(UserFile file) {
        userFileRepository.save(file);
    }

    @Override
    public List<UserFile> getActiveFiles(String uid, String libraryCode) {
        return userFileRepository.findByUidAndDeleteFlagFalseAndLibraryCode(uid, libraryCode);
    }

    @Override
    public void softDeleteFiles(String uid, List<String> filenames, String libraryCode) {
        List<UserFile> files = userFileRepository.findByUidAndDeleteFlagFalseAndLibraryCode(uid, libraryCode);
        files.stream()
                .filter(f -> filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(true);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void restoreFiles(String uid, List<String> filenames, String libraryCode) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        List<UserFile> files = userFileRepository.findByUidAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(uid, sevenDaysAgo, libraryCode);
        files.stream()
                .filter(f -> filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(false);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void markUploadOk(String uid, String filename, String libraryCode) {
        UserFile file = userFileRepository.findByUidAndNameAndLibraryCode(uid, filename, libraryCode);
        if (file != null) {
            file.setUperr(0);
            userFileRepository.save(file);
        }
    }

    @Override
    public List<UserFile> getDeletedFilesWithin7Days(String uid, String libraryCode) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        return userFileRepository.findByUidAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(uid, sevenDaysAgo, libraryCode);
    }

    @Override
    public void deletePermanently(Long fileId, String libraryCode) {
        Optional<UserFile> fileOptional = userFileRepository.findByIdAndDeleteFlagFalseAndLibraryCode(fileId, libraryCode);
        if (fileOptional.isPresent()) {
            userFileRepository.deleteById(fileId);  // 删除文件
        } else {
            throw new RuntimeException("文件不存在或已被删除");
        }
    }

    @Override
    public List<UserFile> getDeletedFilesBefore(Date cutoff, String libraryCode) {
        return userFileRepository.findByDeleteFlagTrueAndCreatedDateBeforeAndLibraryCode(cutoff, libraryCode);
    }

    @Override
    public void deleteFiles(List<UserFile> files) {
        userFileRepository.deleteAll(files);
    }

    @Override
    public long getUserStorageUsage(String uid, String libraryCode) {
        return userFileRepository.findByUidAndDeleteFlagFalseAndLibraryCode(uid, libraryCode)
                .stream()
                .mapToLong(UserFile::getSize)
                .sum();
    }

    @Override
    public List<UserFile> listFilesByFolder(String uid, Long folderId, String libraryCode) {
        return userFileRepository.findByUidAndFolderIdAndDeleteFlagFalseAndLibraryCode(uid, folderId, libraryCode);
    }

    @Override
    public UserFile getFileById(Long fileId, String libraryCode) {
        return userFileRepository.findByIdAndDeleteFlagFalseAndLibraryCode(fileId, libraryCode)
                .orElseThrow(() -> new RuntimeException("文件不存在或已被删除"));
    }

    @Override
    public UserFile uploadNewDocument(MultipartFile file, String uid, String libraryCode, String notes) throws Exception {
        if (!storageQuotaService.canUpload(uid, file.getSize(), libraryCode)) {
            throw new RuntimeException("上传失败：配额不足");
        }

        return uploadFileAndCreateRecord(uid, file, libraryCode, notes);
    }

    @Override
    public UserFile uploadFileAndCreateRecord(String uid, MultipartFile file, String libraryCode, String notes) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String bucketName = minioService.getBucketName(uid, libraryCode);

        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        }

        String objectName = System.currentTimeMillis() + "_" + originalFilename;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(io.minio.PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("User {} uploaded file to bucket {}: {}", uid, bucketName, objectName);
        } catch (MinioException e) {
            log.error("MinIO upload error: ", e);
            throw new Exception("文件上传失败: " + e.getMessage());
        }

        // 使用 buildFileRecord 构建 UserFile 实体
        Long docId = cachedIdGenerator.nextId("doc_id");
        UserFile fileRecord = buildFileRecord(uid, libraryCode, file, objectName, 1, docId, notes, true, bucketName);

        return userFileRepository.save(fileRecord);
    }



    @Transactional
    @Override
    public UserFile uploadNewVersion(MultipartFile file, String uid, String libraryCode, Long docId, String notes) {
        if (!storageQuotaService.canUpload(uid, file.getSize(), libraryCode)) {
            throw new RuntimeException("上传失败：存储配额不足");
        }

        // 获取历史版本，确定版本号
        List<UserFile> history = userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
        int nextVersion = history.isEmpty() ? 1 : (history.get(0).getVersionNumber() + 1);

        // 批量标记旧版本为非最新
        userFileRepository.markAllOldVersionsNotLatest(docId, libraryCode);

        // 上传至 MinIO
        String objectName;
        try {
            objectName = minioService.uploadFile(uid, file, libraryCode);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }

        String bucketName = minioService.getBucketName(uid, libraryCode);
        UserFile newVersion = buildFileRecord(uid, libraryCode, file, objectName, nextVersion, docId, notes, true, bucketName);
        return userFileRepository.save(newVersion);

    }



    @Override
    public List<UserFile> getVersionsByDocId(Long docId, String libraryCode) {
        return userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, String uid, String libraryCode) {
        // 查询 docId + uid + libraryCode 下最新的有效版本（isLatest=true，deleteFlag=false）
        return userFileRepository.findFirstByDocIdAndUidAndLibraryCodeAndIsLatestTrueAndDeleteFlagFalse(docId, uid, libraryCode)
                .orElse(null);
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, String uid) {
        return userFileRepository.findFirstByDocIdAndUidAndIsLatestTrue(docId, uid)
                .orElse(null);
    }

    @Override
    public String generateShareToken(String uid, String filename, Date expireAt) {
        // 查找文件
        UserFile file = userFileRepository.findByUidAndOriginFilenameAndDeleteFlagFalse(uid, filename)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("fileId", file.getId());
        claims.put("fileName", file.getOriginFilename());

        long now = System.currentTimeMillis();
        long expireMillis;
        if (expireAt != null) {
            expireMillis = expireAt.getTime() - now;
            if (expireMillis <= 0) {
                throw new IllegalArgumentException("过期时间必须晚于当前时间");
            }
        } else {
            expireMillis = defaultExpireMillis; // 比如 2小时
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("file_share")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireMillis))
                .signWith(Keys.hmacShaKeyFor(shareSecret.getBytes()), SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public UserFile validateAndGetSharedFile(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(shareSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long fileId = claims.get("fileId", Long.class);

            if (fileId == null) {
                throw new IllegalArgumentException("Token 中缺少文件信息");
            }

            Optional<UserFile> optional = userFileRepository.findById(fileId);
            if (optional.isEmpty()) {
                throw new IllegalArgumentException("文件不存在");
            }

            return optional.get();
        } catch (ExpiredJwtException e) {
            throw new IllegalStateException("分享链接已过期");
        } catch (Exception e) {
            throw new IllegalStateException("无效的分享链接");
        }
    }

    /**
     * 记录用户操作行为
     * @param token 令牌
     * @param request 请求
     * @param actionType
     *         preview: 用户预览文件
     *         list: 用户查看文件列表
     *         delete: 用户删除文件（如果适用）
     *         restore: 用户恢复文件（如果适用）
     *         share: 用户分享文件链接
     */
    @Override
    public void recordShareAccess(String token, HttpServletRequest request, String actionType) {
        try {
            // 解析 token
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(shareSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long fileId = claims.get("fileId", Long.class);
            String fileName = claims.get("fileName", String.class);

            // 校验文件信息
            if (fileId == null || fileName == null) {
                log.warn("访问日志记录失败：缺失 fileId 或 fileName，token 无效");
                return;
            }

            // 生成 token 的哈希值（避免存储敏感信息）
            String tokenHash = DigestUtils.sha256Hex(token);

            // 构建日志对象
            ShareAccessLog logEntry = ShareAccessLog.builder()
                    .token(token)           // 记录原始 token，便于后期追踪
                    .tokenHash(tokenHash)   // 记录哈希值
                    .fileId(fileId)
                    .fileName(fileName)
                    .accessIp(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .accessTime(new Date())
                    .libraryCode(claims.get("libraryCode", String.class))
                    .ownerUid(claims.get("uid", String.class))
                    .actionType(actionType) // 设置操作类型
                    .build();

            // 保存访问日志
            shareAccessLogService.recordAccess(logEntry);
        } catch (ExpiredJwtException e) {
            log.warn("访问日志记录失败：token 已过期");
        } catch (JwtException e) {
            log.warn("访问日志记录失败：token 解析错误", e);
        } catch (Exception e) {
            log.warn("访问日志记录异常", e);
        }
    }


    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim(); // 如果多个 IP，只取第一个
        }
        return ip;
    }


    private UserFile buildFileRecord(String uid, String libraryCode, MultipartFile file, String objectName,
                                     int version, Long docId, String notes, boolean isLatest, String bucketName) {
        UserFile userFile = new UserFile();
        userFile.setUid(uid);
        userFile.setLibraryCode(libraryCode);
        userFile.setName(objectName);
        userFile.setOriginFilename(file.getOriginalFilename());
        userFile.setSize(file.getSize());
        userFile.setVersionNumber(version);
        userFile.setDocId(docId);
        userFile.setIsLatest(isLatest);
        userFile.setNotes(notes);
        userFile.setCreatedDate(new Date());
        userFile.setDeleteFlag(false);
        userFile.setBucket(bucketName);
        userFile.setType(file.getContentType());
        // ✅ 设置 URL（对象名即可，供下载接口使用）
        userFile.setUrl(objectName);
        return userFile;
    }



}
