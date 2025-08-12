package com.github.sdms.service.impl;

import com.github.sdms.dto.UserFilePageRequest;
import com.github.sdms.dto.UserFileSummaryDTO;
import com.github.sdms.dto.UserFileVersionDTO;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.enums.IdType;
import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.BucketPermissionRepository;
import com.github.sdms.repository.FolderRepository;
import com.github.sdms.repository.RolePermissionRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.*;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
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

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private PermissionChecker permissionChecker;


    @Override
    public Page<UserFileSummaryDTO> pageFiles(UserFilePageRequest request, CustomerUserDetails userDetails) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<UserFile> page = userFileRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleteFlag"), false));

            switch (userDetails.getRoleType()) {
                case ADMIN:
                    break;
                case LIBRARIAN:
                case READER:
                default:
                    predicates.add(cb.equal(root.get("libraryCode"), userDetails.getLibraryCode()));
                    predicates.add(cb.equal(root.get("userId"), userDetails.getUserId()));
                    break;
            }
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String likeKeyword = "%" + request.getKeyword() + "%";
                Predicate p1 = cb.like(root.get("originFilename"), likeKeyword);
                predicates.add(p1); // ✅ 文件名模糊搜索
            }

            if (request.getName() != null && !request.getName().isEmpty()) {
                predicates.add(cb.like(root.get("originFilename"), "%" + request.getName() + "%"));
            }
            if (request.getType() != null && !request.getType().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), request.getType()));
            }
            if (request.getFolderId() != null) {
                predicates.add(cb.equal(root.get("folderId"), request.getFolderId()));
            }
            // 其他过滤条件代码后面
            if (request.getFolderId() != null) {
                // 递归查询所有子目录ID
                List<Long> folderIds = folderService.getAllSubFolderIds(request.getFolderId());
                folderIds.add(request.getFolderId()); // 包含自身
                predicates.add(root.get("folderId").in(folderIds));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<UserFileSummaryDTO> dtoList = page.getContent().stream().map(file -> {
            UserFileSummaryDTO dto = new UserFileSummaryDTO();
            dto.setId(file.getId());
            dto.setOriginFilename(file.getOriginFilename());
            dto.setType(file.getType());
            dto.setSize(file.getSize());
            dto.setCreatedDate(file.getCreatedDate());
            dto.setVersionNumber(file.getVersionNumber());
            dto.setIsLatest(file.getIsLatest());
            dto.setShared(file.getShared());
            dto.setFolderId(file.getFolderId());
            return dto;
        }).toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }


    @Override
    public void saveUserFile(UserFile file) {
        userFileRepository.save(file);
    }

    @Override
    public List<UserFile> listFilesByRole(CustomerUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        String libraryCode = userDetails.getLibraryCode();
        RoleType role = userDetails.getRoleType();

        switch (role) {
            case ADMIN:
                return userFileRepository.findByDeleteFlagFalse(); // 所有文件（忽略馆号）

            case LIBRARIAN:
                // 本馆所有上传文件
                List<UserFile> librarianFiles = userFileRepository.findByLibraryCodeAndDeleteFlagFalse(libraryCode);

                // 获取该用户额外有访问权限的文件
                List<UserFile> accessibleByPermission = getPermissionGrantedFiles(userId, libraryCode, role);
                return mergeWithoutDuplicates(librarianFiles, accessibleByPermission);

            case READER:
            default:
                // 自己上传的
                List<UserFile> ownFiles = userFileRepository.findByUserIdAndLibraryCodeAndDeleteFlagFalse(userId, libraryCode);

                // 获取该用户通过权限访问到的其他文件
                List<UserFile> sharedFiles = getPermissionGrantedFiles(userId, libraryCode, role);
                return mergeWithoutDuplicates(ownFiles, sharedFiles);
        }
    }


    @Override
    public void softDeleteFiles(Long userId, List<String> filenames, String libraryCode) {
        List<UserFile> files = userFileRepository.findByUserIdAndDeleteFlagFalseAndLibraryCode(userId, libraryCode);
        files.stream()
                .filter(f -> filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(true);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void restoreFiles(Long userId, List<String> filenames, String libraryCode) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        List<UserFile> files = userFileRepository.findByUserIdAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(userId, sevenDaysAgo, libraryCode);
        files.stream()
                .filter(f -> filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(false);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void markUploadOk(Long userId, String filename, String libraryCode) {
        UserFile file = userFileRepository.findByUserIdAndNameAndLibraryCode(userId, filename, libraryCode);
        if (file != null) {
            file.setUperr(0);
            userFileRepository.save(file);
        }
    }

    @Override
    public List<UserFile> getDeletedFilesWithin7Days(Long userId, String libraryCode) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        return userFileRepository.findByUserIdAndDeleteFlagTrueAndCreatedDateAfterAndLibraryCode(userId, sevenDaysAgo, libraryCode);
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
    public long getUserStorageUsage(Long userId, String libraryCode) {
        return userFileRepository.findByUserIdAndDeleteFlagFalseAndLibraryCode(userId, libraryCode)
                .stream()
                .mapToLong(UserFile::getSize)
                .sum();
    }

    @Override
    public List<UserFile> listFilesByFolder(Long userId, Long folderId, String libraryCode) {
        return userFileRepository.findByUserIdAndFolderIdAndDeleteFlagFalseAndLibraryCode(userId, folderId, libraryCode);
    }

    @Override
    public UserFile getFileById(Long fileId, String libraryCode) {
        // 从上下文获取uid，或者通过参数传入（这里示范参数传入方式）
        // 如无上下文，需在调用处补充uid传递
        Long userId = JwtUtil.getCurrentUserIdOrThrow(); // 需自行实现或传入参数

        if (!permissionValidator.canReadFile(userId, fileId)) {
            throw new ApiException(403, "无权限访问该文件");
        }

        return userFileRepository.findByIdAndDeleteFlagFalseAndLibraryCode(fileId, libraryCode)
                .orElseThrow(() -> new ApiException(404, "指定的文件不存在或已被删除"));
    }

    @Override
    @Transactional
    public UserFile uploadNewDocument(MultipartFile file, Long userId, Bucket targetBucket, String notes, Long folderId) {
        String bucketName = targetBucket.getName();
        String libraryCode = targetBucket.getLibraryCode();
        Long bucketId= targetBucket.getId();   // 新增

        // 构建 MinIO 对象名（避免命名冲突）
        String objectName = FileUtil.generateObjectName(file.getOriginalFilename());

        // 上传文件到 MinIO
        minioService.uploadFile(userId, bucketName, file);

        // 提取文件名
        String originFilename = file.getOriginalFilename();

        // ===== 新增逻辑：判断版本号 =====
        Integer maxVersion = userFileRepository.findMaxVersionNumber(originFilename, folderId, userId, libraryCode);
        int nextVersion = (maxVersion == null ? 1 : maxVersion + 1);

        // ===== 新增逻辑：标记旧版本为非最新 =====
        if (maxVersion != null) {
            userFileRepository.markOldVersionsNotLatest(originFilename, folderId, userId, libraryCode);
        }

        // docId：新文件使用新的 docId（即便同名也是新文档）
        long docId = cachedIdGenerator.nextId(IdType.DOC_ID.name());


        // 构建 UserFile 记录
        UserFile userFile = buildFileRecord(
                userId,
                libraryCode,
                file,
                objectName,
                nextVersion,
                docId,
                notes,
                true, // 是最新版本
                bucketName,
                bucketId,   // 新增
                folderId
        );

        // 保存数据库
        userFileRepository.save(userFile);

        // 自动授权
        if (!permissionValidator.hasWritePermission(userId, bucketName)) {
            permissionService.addBucketPermission(userId, bucketName, PermissionType.WRITE);
        }

        return userFile;
    }

    @Override
    @Transactional
    public List<UserFile> uploadMultipleNewDocuments(List<MultipartFile> files, Long userId, Bucket targetBucket, String notes, Long folderId) {
        String bucketName = targetBucket.getName();
        String libraryCode = targetBucket.getLibraryCode();
        Long bucketId = targetBucket.getId();

        List<UserFile> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String originFilename = file.getOriginalFilename();

            // 构建对象名（唯一标识）
            String objectName = FileUtil.generateObjectName(originFilename);

            // 上传文件
            minioService.uploadFile(userId, bucketName, file);

            // ===== 版本号处理 =====
            Integer maxVersion = userFileRepository.findMaxVersionNumber(originFilename, folderId, userId, libraryCode);
            int nextVersion = (maxVersion == null ? 1 : maxVersion + 1);

            if (maxVersion != null) {
                userFileRepository.markOldVersionsNotLatest(originFilename, folderId, userId, libraryCode);
            }

            // 生成新 docId（即使是同名也视作新文档版本）
            long docId = cachedIdGenerator.nextId(IdType.DOC_ID.name());

            UserFile userFile = buildFileRecord(
                    userId,
                    libraryCode,
                    file,
                    objectName,
                    nextVersion,
                    docId,
                    notes,
                    true, // 是最新版本
                    bucketName,
                    bucketId,    // 新增
                    folderId
            );

            userFileRepository.save(userFile);
            savedFiles.add(userFile);

            // 自动授权（每个文件都检查一次，必要时优化为全局权限判断）
            if (!permissionValidator.hasWritePermission(userId, bucketName)) {
                permissionService.addBucketPermission(userId, bucketName, PermissionType.WRITE);
            }
        }

        return savedFiles;
    }

    @Override
    @Transactional
    public void moveItems(List<Long> fileIds, List<Long> folderIds, Long targetFolderId, Long userId) {
        // ===== 校验目标目录 =====
        Folder targetFolder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new ApiException(404, "目标目录不存在"));
        if (!targetFolder.getUserId().equals(userId)) {
            throw new ApiException(403, "无权限访问目标目录");
        }

        // ===== 处理文件移动 =====
        if (fileIds != null && !fileIds.isEmpty()) {
            List<UserFile> files = userFileRepository.findAllById(fileIds);
            for (UserFile file : files) {
                if (!file.getUserId().equals(userId)) {
                    throw new ApiException(403, "无权限移动文件 ID: " + file.getId());
                }
                if (file.getFolderId() != null && isSubFolder(file.getFolderId(), targetFolderId)) {
                    throw new ApiException(400, "不能将文件移动到其子目录，文件ID: " + file.getId());
                }
                file.setFolderId(targetFolderId);
            }
            userFileRepository.saveAll(files);
        }

        // ===== 处理目录移动 =====
        if (folderIds != null && !folderIds.isEmpty()) {
            List<Folder> folders = folderRepository.findAllById(folderIds);
            for (Folder folder : folders) {
                if (!folder.getUserId().equals(userId)) {
                    throw new ApiException(403, "无权限移动目录 ID: " + folder.getId());
                }
                // 防止移动到自己或子目录
                if (folder.getId().equals(targetFolderId)) {
                    throw new ApiException(400, "不能将目录移动到其自身");
                }
                if (isSubFolder(folder.getId(), targetFolderId)) {
                    throw new ApiException(400, "不能将目录移动到其子目录 ID: " + folder.getId());
                }
                folder.setParentId(targetFolderId);
            }
            folderRepository.saveAll(folders);
        }
    }

    private boolean isSubFolder(Long sourceFolderId, Long targetFolderId) {
        Long currentId = targetFolderId;
        while (currentId != null) {
            if (currentId.equals(sourceFolderId)) {
                return true;
            }
            Folder current = folderRepository.findById(currentId).orElse(null);
            if (current == null) break;
            currentId = current.getParentId();
        }
        return false;
    }

    @Override
    public List<UserFileVersionDTO> getFileVersions(Long docId, Long userId) {
        if (docId == null) {
            throw new ApiException(400, "docId 不能为空");
        }

        List<UserFile> versions = userFileRepository.findByDocIdAndUserIdOrderByVersionNumberDesc(docId, userId);

        if (versions.isEmpty()) {
            throw new ApiException(404, "未找到该文档的任何版本");
        }

        return versions.stream().map(file -> {
            UserFileVersionDTO dto = new UserFileVersionDTO();
            dto.setId(file.getId());
            dto.setVersionNumber(file.getVersionNumber());
            dto.setOriginFilename(file.getOriginFilename());
            dto.setSize(file.getSize());
            dto.setCreatedDate(file.getCreatedDate());
            dto.setIsLatest(file.getIsLatest());
            dto.setVersionKey(file.getVersionKey());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long restoreFileVersion(Long fileId, Long userId) {
        UserFile oldVersion = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(404, "版本文件不存在"));

        if (!oldVersion.getUserId().equals(userId)) {
            throw new ApiException(403, "无权限恢复该版本");
        }

        Long docId = oldVersion.getDocId();
        if (docId == null) {
            throw new ApiException(400, "非法的历史版本记录，缺失 docId");
        }

        // 查找最新版本号
        Integer maxVersion = userFileRepository.findMaxVersionByDocId(docId);
        if (maxVersion == null) {
            maxVersion = 0;
        }

        // 置旧的 isLatest 为 false
        userFileRepository.clearLatestVersionFlag(docId);

        // 创建新版本记录
        UserFile newVersion = new UserFile();
        BeanUtils.copyProperties(oldVersion, newVersion, "id", "versionNumber", "createdDate");

        newVersion.setVersionNumber(maxVersion + 1);
        newVersion.setIsLatest(true);
        newVersion.setCreatedDate(new Date());

        userFileRepository.save(newVersion);
        return newVersion.getId();
    }

    // 3. Service实现（UserFileServiceImpl）
    @Override
    @Transactional
    public UserFile copyFile(String filename, Long userId, String libraryCode, Long targetFolderId) {
        // 查询源文件
        UserFile sourceFile = userFileRepository.findByOriginFilenameAndUserIdAndLibraryCodeAndDeleteFlagFalse(
                filename, userId, libraryCode
        ).orElseThrow(() -> new ApiException(404, "文件不存在"));

        // 权限校验
        permissionChecker.checkFileAccess(userId, sourceFile.getId(), "READ");

        // 校验目标目录权限
        Folder targetFolder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new ApiException(404, "目标目录不存在"));
        if (!targetFolder.getUserId().equals(userId)) {
            throw new ApiException(403, "无权限访问目标目录");
        }

        // 复制存储对象（MinIO）
        String newObjectName = generateNewObjectName(sourceFile.getName());
        minioService.copyObject(sourceFile.getBucket(), sourceFile.getName(), sourceFile.getBucket(), newObjectName);

        // 复制数据库文件记录，版本号重置为1，标记为最新版本
        UserFile copiedFile = new UserFile();
        copiedFile.setOriginFilename(sourceFile.getOriginFilename());
        copiedFile.setName(newObjectName);
        copiedFile.setType(sourceFile.getType());
        copiedFile.setSize(sourceFile.getSize());
        copiedFile.setCreatedDate(new Date());
        copiedFile.setUserId(userId);
        copiedFile.setLibraryCode(libraryCode);
        copiedFile.setVersionNumber(1);
        copiedFile.setIsLatest(true);
        copiedFile.setShared(false);
        copiedFile.setFolderId(targetFolderId);
        copiedFile.setBucket(sourceFile.getBucket());
        copiedFile.setDeleteFlag(false);

        return userFileRepository.save(copiedFile);
    }

    // 辅助生成新对象名（避免重复）
    private String generateNewObjectName(String originalName) {
        String suffix = "";
        String baseName = originalName;
        String ext = "";

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
            ext = originalName.substring(dotIndex);
        }
        suffix = "_" + System.currentTimeMillis();
        return baseName + suffix + ext;
    }


    @Override
    public UserFile uploadFileAndCreateRecord(Long userId, MultipartFile file, String libraryCode, String notes, Long folderId) throws Exception {
        Long bucketId = minioService.getBucketIdForUpload(userId, libraryCode);

        if (!hasBucketPermission(userId, bucketId, "write")) {
            throw new ApiException(403, "无权限上传文件到该桶");
        }
        String originalFilename = file.getOriginalFilename();
        String bucketName = BucketUtil.getBucketName(userId, libraryCode);

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
            log.info("User {} uploaded file to bucket {}: {}", userId, bucketName, objectName);
        } catch (MinioException e) {
            log.error("MinIO upload error: ", e);
            throw new ApiException(500, "文件上传失败：" + e.getMessage());
        }

        Long docId = cachedIdGenerator.nextId("doc_id");
        UserFile fileRecord = buildFileRecord(userId, libraryCode, file, objectName, 1, docId, notes, true, bucketName, bucketId,folderId);
        return userFileRepository.save(fileRecord);
    }

    @Transactional
    @Override
    public UserFile uploadNewVersion(MultipartFile file, Long userId, String libraryCode, Long docId, String notes, Long folderId) throws Exception {
        // 获取文件最新版本id，用于权限校验
        UserFile originFile = getFileByDocIdAndUid(docId, userId, libraryCode);
        String bucketName = originFile.getBucket();  // 保持一致
        Long bucketId = originFile.getBucketId();    // 新增：直接继承原文件的 bucketId
        if (originFile == null) {
            throw new ApiException(403, "无权限上传该文档新版本");
        }

        if (!permissionValidator.canWriteFile(userId, originFile.getId())) {
            throw new ApiException(403, "无权限上传该文档新版本");
        }

        if (!storageQuotaService.canUpload(userId, file.getSize(), libraryCode)) {
            throw new ApiException(403, "上传失败：存储配额不足");
        }

        List<UserFile> history = userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
        int nextVersion = history.isEmpty() ? 1 : (history.get(0).getVersionNumber() + 1);

        userFileRepository.markAllOldVersionsNotLatest(docId, libraryCode);

        String objectName;
        try {
            objectName = minioService.uploadFile(userId, file, libraryCode);
        } catch (Exception e) {
            log.error("上传新版本失败: ", e);
            throw new ApiException(500, "上传新版本失败：" + e.getMessage());
        }

        UserFile newVersion = buildFileRecord(userId, libraryCode, file, objectName, nextVersion, docId, notes, true, bucketName, bucketId,folderId);
        return userFileRepository.save(newVersion);
    }

    @Override
    public List<UserFile> getVersionsByDocId(Long docId, String libraryCode) {
        return userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, Long userId, String libraryCode) {
        UserFile file = userFileRepository.findFirstByDocIdAndUserIdAndLibraryCodeAndIsLatestTrueAndDeleteFlagFalse(docId, userId, libraryCode)
                .orElse(null);

        if (file != null && !permissionValidator.canReadFile(userId, file.getId())) {
            throw new ApiException(403, "无权限访问该文件");
        }
        return file;
    }

    @Override
    public UserFile getFileByDocIdAndUid(Long docId, Long userId) {
        return userFileRepository.findFirstByDocIdAndUserIdAndIsLatestTrue(docId, userId)
                .orElse(null);
    }

    @Override
    public UserFile getFileByName(String filename, Long userId, String libraryCode) {
        UserFile file = userFileRepository.findByUserIdAndNameAndLibraryCode(userId, filename, libraryCode);
        if (file == null || file.getDeleteFlag()) {
            throw new ApiException(404, "文件不存在或已被删除");
        }
        if (!permissionValidator.canReadFile(userId, file.getId())) {
            throw new ApiException(403, "无权限访问该文件");
        }
        return file;
    }

    @Override
    public void softDeleteFile(UserFile file) {
        file.setDeleteFlag(true);
        userFileRepository.save(file);
    }


    private UserFile buildFileRecord(Long userId, String libraryCode, MultipartFile file, String objectName,
                                     int version, Long docId, String notes, boolean isLatest, String bucketName, Long bucketId, Long folderId) {
        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
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
        userFile.setBucketId(bucketId); // 保存 bucketId
        userFile.setType(file.getContentType());
        userFile.setFolderId(folderId);
        userFile.setUrl(objectName);
        return userFile;
    }

    private List<UserFile> getPermissionGrantedFiles(Long userId, String libraryCode, RoleType roleType) {
        // Step 1: 获取用户权限中的桶 ID
        List<Long> permittedBucketIds = bucketPermissionRepository.findBucketIdsByUserId(userId);

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


    private boolean hasBucketPermission(Long userId, Long bucketId, String permission) {
        return bucketPermissionRepository.hasPermission(userId, bucketId, permission);
    }
}
