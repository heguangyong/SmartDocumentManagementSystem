package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.enums.IdType;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.repository.RolePermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.*;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

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

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private BucketService bucketService;

    @Override
    public void saveUserFile(UserFile file) {
        userFileRepository.save(file);
    }

    @Override
    public List<UserFile> listFilesByRole(CustomerUserDetails userDetails) {
        String uid = userDetails.getUid();
        String libraryCode = userDetails.getLibraryCode();
        RoleType role = userDetails.getRoleType();

        switch (role) {
            case ADMIN:
                return userFileRepository.findByDeleteFlagFalse(); // 所有文件（忽略馆号）

            case LIBRARIAN:
                // 本馆所有上传文件
                List<UserFile> librarianFiles = userFileRepository.findByLibraryCodeAndDeleteFlagFalse(libraryCode);

                // 获取该用户额外有访问权限的文件
                List<UserFile> accessibleByPermission = getPermissionGrantedFiles(uid, libraryCode, role);
                return mergeWithoutDuplicates(librarianFiles, accessibleByPermission);

            case READER:
            default:
                // 自己上传的
                List<UserFile> ownFiles = userFileRepository.findByUidAndLibraryCodeAndDeleteFlagFalse(uid, libraryCode);

                // 获取该用户通过权限访问到的其他文件
                List<UserFile> sharedFiles = getPermissionGrantedFiles(uid, libraryCode, role);
                return mergeWithoutDuplicates(ownFiles, sharedFiles);
        }
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
        String uid = AuthUtils.getUid(); // 需自行实现或传入参数

        if (!permissionValidator.canReadFile(uid, fileId)) {
            throw new ApiException(403, "无权限访问该文件");
        }

        return userFileRepository.findByIdAndDeleteFlagFalseAndLibraryCode(fileId, libraryCode)
                .orElseThrow(() -> new ApiException(404, "指定的文件不存在或已被删除"));
    }

    @Override
    public UserFile uploadNewDocument(MultipartFile file, String uid, Bucket targetBucket, String notes, Long folderId) {
        String bucketName = targetBucket.getName();
        String libraryCode = targetBucket.getLibraryCode();

        // 1. 构建 MinIO 对象名
        String objectName = FileUtil.generateObjectName(file.getOriginalFilename());

        // 2. 上传文件至 MinIO
        minioService.uploadFile(bucketName, objectName, file);

        // ✅ 3. 生成新的 docId（首次上传）
        long docId = cachedIdGenerator.nextId(IdType.DOC_ID.name());

        // 4. 构建 UserFile 实体对象（版本默认 1，首次上传）
        UserFile userFile = buildFileRecord(
                uid,
                libraryCode,
                file,
                objectName,
                1,         // 初始版本号
                docId,     // ✅ 设置新生成的 docId
                notes,
                true,      // 是最新版本
                bucketName,
                folderId
        );

        // 5. 保存数据库记录
        userFileRepository.save(userFile);

        // 6. 自动授权上传者权限（如果需要）
        if (!permissionValidator.hasWritePermission(uid, bucketName)) {
            permissionService.addBucketPermission(uid, bucketName, PermissionType.WRITE);
        }

        return userFile;
    }




    @Override
    public UserFile uploadFileAndCreateRecord(String uid, MultipartFile file, String libraryCode, String notes, Long folderId) throws Exception {
        Long bucketId = minioService.getBucketIdForUpload(uid, libraryCode);

        if (!hasBucketPermission(uid, bucketId, "write")) {
            throw new ApiException(403, "无权限上传文件到该桶");
        }
        String originalFilename = file.getOriginalFilename();
        String bucketName = BucketUtil.getBucketName(uid, libraryCode);

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

        String bucketName = BucketUtil.getBucketName(uid, libraryCode);
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

    @Override
    public UserFile getFileByName(String filename, String uid, String libraryCode) {
        UserFile file = userFileRepository.findByUidAndNameAndLibraryCode(uid, filename, libraryCode);
        if (file == null || file.getDeleteFlag()) {
            throw new ApiException(404, "文件不存在或已被删除");
        }
        if (!permissionValidator.canReadFile(uid, file.getId())) {
            throw new ApiException(403, "无权限访问该文件");
        }
        return file;
    }

    @Override
    public void softDeleteFile(UserFile file) {
        file.setDeleteFlag(true);
        userFileRepository.save(file);
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

    private List<UserFile> getPermissionGrantedFiles(String uid, String libraryCode, RoleType roleType) {
        // Step 1: 获取用户权限中的桶 ID
        List<Long> permittedBucketIds = bucketPermissionRepository.findBucketIdsByUid(uid);

        // Step 2: 获取该角色被授权的桶 ID
        List<Long> roleBucketIds = rolePermissionRepository.findBucketResourceIdsByRoleType(roleType);

        // 合并
        Set<Long> allBucketIds = new HashSet<>();
        allBucketIds.addAll(permittedBucketIds);
        allBucketIds.addAll(roleBucketIds);

        if (allBucketIds.isEmpty()) return Collections.emptyList();

        // 查询桶名
        List<String> bucketNames = bucketService.findBucketNamesByIds(allBucketIds);
        if (bucketNames.isEmpty()) return Collections.emptyList();

        // 查询这些桶中的文件
        return userFileRepository.findByBucketInAndDeleteFlagFalse(new HashSet<>(bucketNames));
    }



    private List<UserFile> mergeWithoutDuplicates(List<UserFile> a, List<UserFile> b) {
        Map<Long, UserFile> map = new HashMap<>();
        for (UserFile f : a) map.put(f.getId(), f);
        for (UserFile f : b) map.put(f.getId(), f);
        return new ArrayList<>(map.values());
    }


    private boolean hasBucketPermission(String uid, Long bucketId, String permission) {
        return bucketPermissionRepository.hasPermission(uid, bucketId, permission);
    }
}
