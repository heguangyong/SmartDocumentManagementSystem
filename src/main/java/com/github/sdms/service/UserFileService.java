package com.github.sdms.service;

import com.github.sdms.dto.UserFileDTO;
import com.github.sdms.dto.UserFilePageRequest;
import com.github.sdms.dto.UserFileSummaryDTO;
import com.github.sdms.dto.UserFileVersionDTO;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.UserFile;
import com.github.sdms.util.CustomerUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface UserFileService {

    Page<UserFileSummaryDTO> pageFiles(UserFilePageRequest request, CustomerUserDetails userDetails);

    void moveItems(List<Long> fileIds, List<Long> folderIds, Long targetFolderId, Long userId);

    List<UserFileVersionDTO> getFileVersions(Long docId, Long userId);

    Long restoreFileVersion(Long fileId, Long userId);

    // 2. Service接口补充
    UserFile copyFile(Long fileId, Long userId, String libraryCode, Long targetFolderId);


    void saveUserFile(UserFile file);

    List<UserFile> listFilesByRole(CustomerUserDetails userDetails);

    void softDeleteFiles(Long userId, List<String> filenames, String libraryCode);

    void restoreFiles(Long userId, List<String> filenames, String libraryCode);

    void markUploadOk(Long userId, String filename, String libraryCode);

    List<UserFile> getDeletedFilesWithin7Days(Long userId, String libraryCode);

    void deletePermanently(Long fileId, String libraryCode);

    List<UserFile> getDeletedFilesBefore(Date cutoff, String libraryCode);

    void deleteFiles(List<UserFile> files);

    long getUserStorageUsage(Long userId, String libraryCode);

    List<UserFile> listFilesByFolder(Long userId, Long folderId, String libraryCode);

    UserFile getFileById(Long fileId);

    UserFile getFileByIdIgnorePermission(Long fileId);

    UserFile uploadNewDocument(MultipartFile file, Long userId, Bucket targetBucket, String notes, Long folderId) throws Exception;

    UserFile uploadFileAndCreateRecord(Long userId, MultipartFile file, String libraryCode, String notes, Long folderId) throws Exception;

    UserFile uploadNewVersion(MultipartFile file, Long userId, String libraryCode, Long docId, String notes, Long folderId) throws Exception;

    List<UserFile> getVersionsByDocId(Long docId, String libraryCode);

    UserFile getFileByDocIdAndUid(Long docId, Long userId, String libraryCode);

    UserFile getFileByDocIdAndLibraryCodeLatest(Long docId, String libraryCode, Long userId);

    UserFile getFileByDocIdAndUid(Long docId, Long userId);

    void softDeleteFile(UserFile file);

    UserFile getFileByName(String filename, Long userId, String libraryCode);

    List<UserFile> uploadMultipleNewDocuments(List<MultipartFile> files, Long userId, Bucket targetBucket, String notes, Long folderId);

    List<UserFileDTO> getVersionsByDocId(Long docId, String libraryCode, Long bucketId, Long folderId);

    UserFileDTO toDTO(UserFile f);

    List<UserFileDTO> toDTOList(List<UserFile> files);

    UserFile uploadNewVersion(MultipartFile file, Long userId, String libraryCode, Long docId, String notes, Long folderId, Bucket targetBucket) throws Exception;

    /**
     * 上传新版本 - 支持InputStream（用于OnlyOffice回调）
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @param libraryCode 图书馆代码
     * @param docId 文档ID
     * @param notes 版本说明
     * @param folderId 文件夹ID（可为null）
     * @return 新版本文件记录
     */
    UserFile uploadNewVersion(InputStream inputStream, String originalFilename,
                              Long userId, String libraryCode, Long docId,
                              String notes, Long folderId) throws Exception;

    List<UserFile> listFilesByFolderId(Long userId, Long folderId, String libraryCode);

    List<UserFile> listRootFilesByBucket(Long userId, Long bucketId, String libraryCode);

    UserFile getHighestVersionFile(Long docId, Long userId, String libraryCode);

    List<UserFile> listFilesByUser(Long userId, String libraryCode);

    // 获取桶根目录的文件（不按 userId 过滤）
    List<UserFile> listRootFilesByBucket(Long bucketId, String libraryCode);

    // 获取某个文件夹下的文件（不按 userId 过滤）
    List<UserFile> listFilesByFolderId(Long folderId, String libraryCode);
}
