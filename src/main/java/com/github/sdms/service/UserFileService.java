package com.github.sdms.service;

import com.github.sdms.model.UserFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface UserFileService {

    void saveUserFile(UserFile file);

    List<UserFile> getActiveFiles(String uid, String libraryCode);

    void softDeleteFiles(String uid, List<String> filenames, String libraryCode);

    void restoreFiles(String uid, List<String> filenames, String libraryCode);

    void markUploadOk(String uid, String filename, String libraryCode);

    List<UserFile> getDeletedFilesWithin7Days(String uid, String libraryCode);

    void deletePermanently(Long fileId, String libraryCode);

    List<UserFile> getDeletedFilesBefore(Date cutoff, String libraryCode);

    void deleteFiles(List<UserFile> files);

    long getUserStorageUsage(String uid, String libraryCode);

    List<UserFile> listFilesByFolder(String uid, Long folderId, String libraryCode);

    UserFile getFileById(Long fileId, String libraryCode);

    UserFile uploadNewDocument(MultipartFile file, String uid, String libraryCode, String notes) throws Exception;

    UserFile uploadFileAndCreateRecord(String uid, MultipartFile file, String libraryCode, String notes) throws Exception;

    UserFile uploadNewVersion(MultipartFile file, String uid, String libraryCode, Long docId, String notes);

    List<UserFile> getVersionsByDocId(Long docId, String libraryCode);

    /**
     * 根据 docId 和 uid 查询最新的用户文件版本
     */
    UserFile getFileByDocIdAndUid(Long docId, String uid, String libraryCode);

    /**
     * 根据文档ID和用户ID获取对应的最新 UserFile 记录
     * @param docId 文档ID
     * @param uid 用户ID
     * @return UserFile，若无则返回null
     */
    UserFile getFileByDocIdAndUid(Long docId, String uid);
}
