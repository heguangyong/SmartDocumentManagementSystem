package com.github.sdms.service;

import com.github.sdms.model.Folder;

import java.util.List;

public interface FolderService {

    Folder createFolder(Long userId, String name, Long parentId, String libraryCode);

    Folder renameFolder(Long userId, Long folderId, String newName, String libraryCode);

    void deleteFolder(Long userId, Long folderId, String libraryCode);

    List<Folder> listFolders(Long userId, Long parentId, String libraryCode);

    List<Folder> listAllFolders(Long userId, String libraryCode);

    Folder getFolderById(Long userId, Long folderId, String libraryCode);

    void moveFolder(Long userId, Long folderId, Long newParentId, String libraryCode);

}
