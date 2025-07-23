package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.PermissionValidator;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CachedIdGenerator;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserFileServiceImpl implements UserFileService {

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
    private long defaultExpireMillis;

    @Autowired
    private PermissionValidator permissionValidator;

    @Autowired
    private BucketPermissionRepository bucketPermissionRepository;

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
            userFileRepository.deleteById(fileId);
        } else {
            throw new ApiException(404, "文件不存在或已被删除，无法永久删除");
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
        // 从上下文获取uid，或者通过参数传入（这里示范参数传入方式）
        // 如无上下文，需在调用处补充uid传递
        String uid = getCurrentUid(); // 需自行实现或传入参数

        if (!permissionValidator.canReadFile(uid, fileId)) {
            throw new ApiException(403, "无权限访问该文件");
        }

        return userFileRepository.findByIdAndDeleteFlagFalseAndLibraryCode(fileId, libraryCode)
                .orElseThrow(() -> new ApiException(404, "指定的文件不存在或已被删除"));
    }

    @Override
    public UserFile uploadNewDocument(MultipartFile file, String uid, String libraryCode, String notes, Long folderId) throws Exception {
        if (!storageQuotaService.canUpload(uid, file.getSize(), libraryCode)) {
            throw new ApiException(403, "上传失败：存储配额不足");
        }

        return uploadFileAndCreateRecord(uid, file, libraryCode, notes, folderId);
    }

    @Override
    public UserFile uploadFileAndCreateRecord(String uid, MultipartFile file, String libraryCode, String notes, Long folderId) throws Exception {
        Long bucketId = minioService.getBucketId(uid, libraryCode);

        if (!hasBucketPermission(uid, bucketId, "write")) {
            throw new ApiException(403, "无权限上传文件到该桶");
        }
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
            throw new ApiException(500, "文件上传失败：" + e.getMessage());
        }

        Long docId = cachedIdGenerator.nextId("doc_id");
        UserFile fileRecord = buildFileRecord(uid, libraryCode, file, objectName, 1, docId, notes, true, bucketName, folderId);
        return userFileRepository.save(fileRecord);
    }

    @Transactional
    @Override
    public UserFile uploadNewVersion(MultipartFile file, String uid, String libraryCode, Long docId, String notes, Long folderId) throws Exception {
        // 获取文件最新版本id，用于权限校验
        UserFile originFile = getFileByDocIdAndUid(docId, uid, libraryCode);
        if (originFile == null) {
            throw new ApiException(403, "无权限上传该文档新版本");
        }

        if (!permissionValidator.canWriteFile(uid, originFile.getId())) {
            throw new ApiException(403, "无权限上传该文档新版本");
        }

        if (!storageQuotaService.canUpload(uid, file.getSize(), libraryCode)) {
            throw new ApiException(403, "上传失败：存储配额不足");
        }

        List<UserFile> history = userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
        int nextVersion = history.isEmpty() ? 1 : (history.get(0).getVersionNumber() + 1);

        userFileRepository.markAllOldVersionsNotLatest(docId, libraryCode);

        String objectName;
        try {
            objectName = minioService.uploadFile(uid, file, libraryCode);
        } catch (Exception e) {
            log.error("上传新版本失败: ", e);
            throw new ApiException(500, "上传新版本失败：" + e.getMessage());
        }

        String bucketName = minioService.getBucketName(uid, libraryCode);
        UserFile newVersion = buildFileRecord(uid, libraryCode, file, objectName, nextVersion, docId, notes, true, bucketName, folderId);
        return userFileRepository.save(newVersion);
    }

    @Override
    public List<UserFile> getVersionsByDocId(Long docId, String libraryCode) {
        return userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, String uid, String libraryCode) {
        UserFile file = userFileRepository.findFirstByDocIdAndUidAndLibraryCodeAndIsLatestTrueAndDeleteFlagFalse(docId, uid, libraryCode)
                .orElse(null);

        if (file != null && !permissionValidator.canReadFile(uid, file.getId())) {
            throw new ApiException(403, "无权限访问该文件");
        }
        return file;
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, String uid) {
        return userFileRepository.findFirstByDocIdAndUidAndIsLatestTrue(docId, uid)
                .orElse(null);
    }

    private UserFile buildFileRecord(String uid, String libraryCode, MultipartFile file, String objectName,
                                     int version, Long docId, String notes, boolean isLatest, String bucketName, Long folderId) {
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
        userFile.setFolderId(folderId);
        userFile.setUrl(objectName);
        return userFile;
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ApiException("未登录，无法获取当前用户");
        }
        return auth.getName(); // 或根据你自定义的 UserDetails 实现类转换后获取 uid
    }

    private boolean hasBucketPermission(String uid, Long bucketId, String permission) {
        return bucketPermissionRepository.hasPermission(uid, bucketId, permission);
    }
}
