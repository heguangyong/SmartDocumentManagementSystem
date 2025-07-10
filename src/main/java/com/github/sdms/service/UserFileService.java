package com.github.sdms.service;

import com.github.sdms.model.UserFile;

import java.util.Date;
import java.util.List;

public interface UserFileService {
    void saveUserFile(UserFile file);

    List<UserFile> getActiveFiles(String uid);

    void softDeleteFiles(String uid, List<String> filenames);

    void restoreFiles(String uid, List<String> filenames);

    void markUploadOk(String uid, String filename);

    List<UserFile> getDeletedFilesWithin7Days(String uid);

    void deletePermanently(Long fileId);

    List<UserFile> getDeletedFilesBefore(Date cutoff);

    void deleteFiles(List<UserFile> files);

    long getUserStorageUsage(String uid); //


}