package com.github.sdms.service;

import com.github.sdms.model.Folder;

import java.util.List;
import java.util.Optional;

public interface FolderService {

    Folder createFolder(String uid, String name, Long parentId, String libraryCode);

    Folder renameFolder(String uid, Long folderId, String newName, String libraryCode);

    void deleteFolder(String uid, Long folderId, String libraryCode);

    List<Folder> listFolders(String uid, Long parentId, String libraryCode);

    List<Folder> listAllFolders(String uid, String libraryCode);

    Folder getFolderById(String uid, Long folderId, String libraryCode);

    void moveFolder(String uid, Long folderId, Long newParentId, String libraryCode);

}
