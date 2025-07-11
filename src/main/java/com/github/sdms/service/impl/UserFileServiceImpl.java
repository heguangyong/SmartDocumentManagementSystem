package com.github.sdms.service.impl;

import com.github.sdms.model.UserFile;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserFileServiceImpl implements UserFileService {

    @Autowired
    private UserFileRepository userFileRepository;

    @Override
    public void saveUserFile(UserFile file) {
        userFileRepository.save(file);
    }

    @Override
    public List<UserFile> getActiveFiles(String uid) {
        return userFileRepository.findByUidAndDeleteFlagFalse(uid);
    }

    @Override
    public void softDeleteFiles(String uid, List<String> filenames) {
        List<UserFile> files = userFileRepository.findAll();
        files.stream()
                .filter(f -> f.getUid().equals(uid) && filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(true);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void restoreFiles(String uid, List<String> filenames) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        List<UserFile> files = userFileRepository.findByUidAndDeleteFlagTrueAndCreatedDateAfter(uid, sevenDaysAgo);
        files.stream()
                .filter(f -> filenames.contains(f.getName()))
                .forEach(f -> {
                    f.setDeleteFlag(false);
                    userFileRepository.save(f);
                });
    }

    @Override
    public void markUploadOk(String uid, String filename) {
        UserFile file = userFileRepository.findByUidAndName(uid, filename);
        if (file != null) {
            file.setUperr(0);
            userFileRepository.save(file);
        }
    }

    @Override
    public List<UserFile> getDeletedFilesWithin7Days(String uid) {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        return userFileRepository.findByUidAndDeleteFlagTrueAndCreatedDateAfter(uid, sevenDaysAgo);
    }

    @Override
    public void deletePermanently(Long fileId) {
        userFileRepository.deleteById(fileId);
    }

    @Override
    public List<UserFile> getDeletedFilesBefore(Date cutoff) {
        return userFileRepository.findByDeleteFlagTrueAndCreatedDateBefore(cutoff);
    }

    @Override
    public void deleteFiles(List<UserFile> files) {
        userFileRepository.deleteAll(files);
    }

    @Override
    public long getUserStorageUsage(String uid) {
        return userFileRepository.findByUidAndDeleteFlagFalse(uid)
                .stream()
                .mapToLong(UserFile::getSize)
                .sum();
    }

    @Override
    public List<UserFile> listFilesByFolder(String uid, Long folderId) {
        return userFileRepository.findByUidAndFolderIdAndDeleteFlagFalse(uid, folderId);
    }

    @Override
    public UserFile getFileById(Long fileId) {
        return userFileRepository.findByIdAndDeleteFlagFalse(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在或已被删除"));
    }


}