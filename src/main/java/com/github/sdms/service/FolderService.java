package com.github.sdms.service;

import com.github.sdms.model.Folder;

import java.util.List;

public interface FolderService {

    Folder createFolder(String uid, String name, Long parentId);

    Folder renameFolder(String uid, Long folderId, String newName);

    void deleteFolder(String uid, Long folderId);

    List<Folder> listFolders(String uid, Long parentId);

    List<Folder> listAllFolders(String uid); // 返回所有目录，便于构建目录树

    Folder getFolderById(String uid, Long folderId);

    void moveFolder(String uid, Long folderId, Long newParentId);

    String generateShareToken(String uid, Long folderId);

    void revokeShareToken(String uid, Long folderId);

    Folder getFolderByShareToken(String token);

}
