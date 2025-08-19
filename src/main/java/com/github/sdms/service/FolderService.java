package com.github.sdms.service;

import com.github.sdms.dto.FolderPageRequest;
import com.github.sdms.dto.FolderSummaryDTO;
import com.github.sdms.model.Folder;
import com.github.sdms.util.CustomerUserDetails;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

public interface FolderService {

    Page<FolderSummaryDTO> pageFolders(FolderPageRequest request, CustomerUserDetails userDetails);

    Folder createFolder(Long userId, String name, Long parentId, String libraryCode);

    Folder renameFolder(Long userId, Long folderId, String newName, String libraryCode);

    void deleteFolder(Long userId, Long folderId, String libraryCode);

    List<Folder> listFolders(Long userId, Long parentId, String libraryCode);

    List<Folder> listAllFolders(Long userId, String libraryCode);

    Folder getFolderById(Long userId, Long folderId, String libraryCode);

    void moveFolder(Long userId, Long folderId, Long newParentId, String libraryCode);

    List<Long> getAllSubFolderIds(Long folderId);

    List<Folder> listFoldersByParentId(Long userId, Long folderId, String libraryCode);

    List<Folder> listRootFoldersByBucket(Long userId, Long bucketId, String libraryCode);

    List<Folder> listAllFoldersByBucket(Long userId, Long bucketId, String libraryCode);

    Collection<Long> getAllDescendantIds(Long excludeFolderId, List<Folder> allFolders);
}
