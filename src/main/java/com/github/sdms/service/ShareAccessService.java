package com.github.sdms.service;

import com.github.sdms.model.ShareAccess;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.Folder;

import java.util.Date;
import java.util.List;

public interface ShareAccessService {

    /**
     * 创建文件分享
     */
    ShareAccess createFileShare(String uid, Long fileId, Integer expireMinutes, String libraryCode);

    /**
     * 创建目录分享
     */
    ShareAccess createFolderShare(String uid, Long folderId, Integer expireMinutes, String libraryCode);

    /**
     * 统一分享创建（动态处理类型）
     */
    String createShare(String uid, String targetType, Long targetId, Date expireAt, String libraryCode);

    /**
     * 撤销分享（逻辑删除或标记无效）
     */
    void revokeShare(String uid, String token, String libraryCode);

    /**
     * 校验token，返回分享记录（已校验过期状态）
     */
    ShareAccess getByToken(String token, String libraryCode);

    /**
     * 仅查询token记录（不校验状态）
     */
    ShareAccess getRawByToken(String token);

    /**
     * 根据token获取文件（必须为 file 类型）
     */
    UserFile getFileByToken(String token, String libraryCode);

    /**
     * 根据token获取目录（必须为 folder 类型）
     */
    Folder getFolderByToken(String token, String libraryCode);

    /**
     * 是否已过期
     */
    boolean isShareExpired(ShareAccess access);

    /**
     * 目录下的文件列表
     */
    List<UserFile> listFilesByFolder(Folder folder);

    /**
     * 目录下的子目录列表
     */
    List<Folder> listChildFolders(Folder folder);

    /**
     * 校验 fileId 是否属于当前分享目录
     */
    UserFile getFileByIdAndValidate(Folder folder, Long fileId);

    /**
     * 获取当前用户创建的所有分享（可筛选类型）
     */
    List<ShareAccess> listMyShares(String uid, String targetType, String libraryCode);
}
