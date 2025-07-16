package com.github.sdms.service.impl;

import com.github.sdms.model.UserFile;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.StorageQuotaService;
import com.github.sdms.service.UserFileService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserFileServiceImpl implements UserFileService {

    @Autowired
    private UserFileRepository userFileRepository;

    @Autowired
    private MinioService minioService;

    @Autowired
    private StorageQuotaService storageQuotaService;



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
    public UserFile uploadNewVersion(MultipartFile file, String uid, String libraryCode, Long docId, String notes) {
        // 先校验配额
        if (!storageQuotaService.canUpload(uid, file.getSize(), libraryCode)) {
            throw new RuntimeException("上传失败：存储配额不足");
        }

        List<UserFile> history = userFileRepository.findByDocIdAndLibraryCodeOrderByVersionNumberDesc(docId, libraryCode);
        int nextVersion = history.isEmpty() ? 1 : history.get(0).getVersionNumber() + 1;

        history.forEach(f -> {
            if (Boolean.TRUE.equals(f.getIsLatest())) {
                f.setIsLatest(false);
                userFileRepository.save(f);
            }
        });

        String objectName;
        try {
            objectName = minioService.uploadFile(uid, file, libraryCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        UserFile newVersion = userFileRepository.findByNameAndLibraryCode(objectName, libraryCode)
                .orElseThrow(() -> new RuntimeException("上传记录未找到"));

        newVersion.setDocId(docId);
        newVersion.setVersionNumber(nextVersion);
        newVersion.setNotes(notes);
        newVersion.setIsLatest(true);
        userFileRepository.save(newVersion);

        return newVersion; // ✅ 别忘了这一句
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
}
