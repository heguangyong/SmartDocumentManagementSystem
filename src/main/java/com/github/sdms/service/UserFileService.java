package com.github.sdms.service;

import com.github.sdms.dto.UserFilePageRequest;
import com.github.sdms.dto.UserFileSummaryDTO;
import com.github.sdms.dto.UserFileVersionDTO;
import com.github.sdms.model.Bucket;
import com.github.sdms.model.UserFile;
import com.github.sdms.util.CustomerUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface UserFileService {

    Page<UserFileSummaryDTO> pageFiles(UserFilePageRequest request, CustomerUserDetails userDetails);

    void moveItems(List<Long> fileIds, List<Long> folderIds, Long targetFolderId, Long userId);

    List<UserFileVersionDTO> getFileVersions(Long docId, Long userId);

    Long restoreFileVersion(Long fileId, Long userId);

    // 2. Service接口补充
    UserFile copyFile(String filename, Long userId, String libraryCode, Long targetFolderId);


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

    UserFile uploadNewDocument(MultipartFile file, Long userId, Bucket targetBucket, String notes, Long folderId) throws Exception;

    UserFile uploadFileAndCreateRecord(Long userId, MultipartFile file, String libraryCode, String notes, Long folderId) throws Exception;

    UserFile uploadNewVersion(MultipartFile file, Long userId, String libraryCode, Long docId, String notes, Long folderId) throws Exception;

    List<UserFile> getVersionsByDocId(Long docId, String libraryCode);

    UserFile getFileByDocIdAndUid(Long docId, Long userId, String libraryCode);

    UserFile getFileByDocIdAndUid(Long docId, Long userId);

    void softDeleteFile(UserFile file);

    UserFile getFileByName(String filename, Long userId, String libraryCode);

    List<UserFile> uploadMultipleNewDocuments(List<MultipartFile> files, Long userId, Bucket targetBucket, String notes, Long folderId);
}
