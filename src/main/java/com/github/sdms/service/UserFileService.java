package com.github.sdms.service;

import com.github.sdms.model.UserFile;

import java.util.Date;
import java.util.List;

/**
 * 用户文件服务接口，包含上传、删除、恢复、配额、目录查询等功能。
 */
public interface UserFileService {

    /** 保存文件记录到数据库 */
    void saveUserFile(UserFile file);

    /** 获取用户当前所有未删除的文件 */
    List<UserFile> getActiveFiles(String uid);

    /** 逻辑删除指定文件（进入回收站） */
    void softDeleteFiles(String uid, List<String> filenames);

    /** 从回收站恢复文件 */
    void restoreFiles(String uid, List<String> filenames);

    /** 标记上传成功的文件（用于异步上传回调） */
    void markUploadOk(String uid, String filename);

    /** 获取最近7天内被逻辑删除的文件（用于回收站显示） */
    List<UserFile> getDeletedFilesWithin7Days(String uid);

    /** 永久删除指定文件（管理员或彻底删除操作） */
    void deletePermanently(Long fileId);

    /** 获取某个时间点之前的逻辑删除文件（用于定期清理） */
    List<UserFile> getDeletedFilesBefore(Date cutoff);

    /** 批量永久删除文件记录 */
    void deleteFiles(List<UserFile> files);

    /** 获取用户当前总存储使用空间（单位：字节） */
    long getUserStorageUsage(String uid);

    /** ✅ 获取指定目录下的用户文件列表（只返回未删除的） */
    List<UserFile> listFilesByFolder(String ownerId, Long folderId);

    UserFile getFileById(Long fileId);

}
