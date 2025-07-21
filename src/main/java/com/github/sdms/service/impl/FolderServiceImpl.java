package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Folder;
import com.github.sdms.repository.FolderRepository;
import com.github.sdms.service.FolderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;

    @Override
    public Folder createFolder(String uid, String name, Long parentId, String libraryCode) {
        List<Folder> siblings = (parentId == null)
                ? folderRepository.findByUidAndParentIdIsNullAndLibraryCode(uid, libraryCode)
                : folderRepository.findByUidAndParentIdAndLibraryCode(uid, parentId, libraryCode);

        if (siblings.stream().anyMatch(f -> f.getName().equals(name))) {
            throw new ApiException(400, "该目录下已存在同名文件夹");
        }

        Folder folder = Folder.builder()
                .uid(uid)
                .name(name)
                .parentId(parentId)
                .libraryCode(libraryCode)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        return folderRepository.save(folder);
    }

    @Override
    public Folder renameFolder(String uid, Long folderId, String newName, String libraryCode) {
        Folder folder = getFolderById(uid, folderId, libraryCode);

        List<Folder> siblings = (folder.getParentId() == null)
                ? folderRepository.findByUidAndParentIdIsNullAndLibraryCode(uid, libraryCode)
                : folderRepository.findByUidAndParentIdAndLibraryCode(uid, folder.getParentId(), libraryCode);

        if (siblings.stream().anyMatch(f -> !f.getId().equals(folderId) && f.getName().equals(newName))) {
            throw new ApiException(400, "该目录下已存在同名文件夹");
        }

        folder.setName(newName);
        folder.setUpdatedAt(new Date());
        return folderRepository.save(folder);
    }

    @Override
    public void deleteFolder(String uid, Long folderId, String libraryCode) {
        Folder folder = getFolderById(uid, folderId, libraryCode);
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new ApiException(403, "系统内置目录禁止删除");
        }
        folderRepository.delete(folder);
    }

    @Override
    public List<Folder> listFolders(String uid, Long parentId, String libraryCode) {
        return (parentId == null)
                ? folderRepository.findByUidAndParentIdIsNullAndLibraryCode(uid, libraryCode)
                : folderRepository.findByUidAndParentIdAndLibraryCode(uid, parentId, libraryCode);
    }

    @Override
    public List<Folder> listAllFolders(String uid, String libraryCode) {
        return folderRepository.findByUidAndLibraryCode(uid, libraryCode);
    }

    @Override
    public Folder getFolderById(String uid, Long folderId, String libraryCode) {
        Folder folder = folderRepository.findByIdAndUidAndLibraryCode(folderId, uid, libraryCode);
        if (folder == null) {
            throw new ApiException(404, "目录不存在或无权限访问");
        }
        return folder;
    }

    @Override
    public void moveFolder(String uid, Long folderId, Long newParentId, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ApiException(404, "待移动目录不存在"));

        Folder newParent = folderRepository.findById(newParentId)
                .orElseThrow(() -> new ApiException(404, "目标父目录不存在"));

        if (!folder.getUid().equals(uid) || !newParent.getUid().equals(uid)) {
            throw new ApiException(403, "无权限操作该目录");
        }

        if (isDescendant(folderId, newParentId)) {
            throw new ApiException(400, "不能将目录移动到其子目录下");
        }

        folder.setParentId(newParentId);
        folder.setUpdatedAt(new Date());
        folderRepository.save(folder);
    }

    private boolean isDescendant(Long sourceId, Long targetParentId) {
        Long currentId = targetParentId;
        while (currentId != null) {
            if (currentId.equals(sourceId)) return true;
            Optional<Folder> parent = folderRepository.findById(currentId);
            if (parent.isEmpty()) break;
            currentId = parent.get().getParentId();
        }
        return false;
    }
}
