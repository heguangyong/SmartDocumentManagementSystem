package com.github.sdms.service.impl;

import com.github.sdms.model.UserFile;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CachedIdGenerator;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
        return userFile;
    }



}
