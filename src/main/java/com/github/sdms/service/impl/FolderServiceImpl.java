package com.github.sdms.service.impl;

import com.github.sdms.model.Folder;
import com.github.sdms.repository.FolderRepository;
import com.github.sdms.service.FolderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;

    @Override
    public Folder createFolder(String uid, String name, Long parentId, String libraryCode) {
        List<Folder> siblings = parentId == null
                ? folderRepository.findByUidAndParentIdIsNullAndLibraryCode(uid, libraryCode)
                : folderRepository.findByUidAndParentIdAndLibraryCode(uid, parentId, libraryCode);
        if (siblings.stream().anyMatch(f -> f.getName().equals(name))) {
            throw new IllegalArgumentException("该目录下已存在同名文件夹");
        }

        Folder folder = Folder.builder()
                .uid(uid)
                .name(name)
                .parentId(parentId)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        return folderRepository.save(folder);
    }

    @Override
    public Folder renameFolder(String uid, Long folderId, String newName, String libraryCode) {
        Folder folder = getFolderById(uid, folderId, libraryCode);
        List<Folder> siblings = folder.getParentId() == null
                ? folderRepository.findByUidAndParentIdIsNullAndLibraryCode(uid, libraryCode)
                : folderRepository.findByUidAndParentIdAndLibraryCode(uid, folder.getParentId(), libraryCode);
        if (siblings.stream().anyMatch(f -> !f.getId().equals(folderId) && f.getName().equals(newName))) {
            throw new IllegalArgumentException("该目录下已存在同名文件夹");
        }

        folder.setName(newName);
        folder.setUpdatedAt(new Date());
        return folderRepository.save(folder);
    }

    @Override
    public void deleteFolder(String uid, Long folderId, String libraryCode) {
        Folder folder = getFolderById(uid, folderId, libraryCode);
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new IllegalStateException("系统内置目录禁止删除");
        }
        folderRepository.delete(folder);
    }

    @Override
    public List<Folder> listFolders(String uid, Long parentId, String libraryCode) {
        return parentId == null
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
            throw new EntityNotFoundException("目录不存在或无权限访问");
        }
        return folder;
    }

    @Override
    public void moveFolder(String uid, Long folderId, Long newParentId, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("待移动目录不存在"));

        Folder newParent = folderRepository.findById(newParentId)
                .orElseThrow(() -> new RuntimeException("目标父目录不存在"));

        if (!folder.getOwnerId().equals(uid) || !newParent.getOwnerId().equals(uid)) {
            throw new SecurityException("无权限操作该目录");
        }

        if (isDescendant(folderId, newParentId)) {
            throw new IllegalArgumentException("不能将目录移动到其子目录下");
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

    @Override
    public String generateShareToken(String uid, Long folderId, Integer expireMinutes, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("目录不存在"));

        if (!folder.getOwnerId().equals(uid)) {
            throw new SecurityException("无权限分享该目录");
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        folder.setShared(true);
        folder.setShareToken(token);

        if (expireMinutes != null && expireMinutes > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, expireMinutes);
            folder.setShareExpireAt(calendar.getTime());
        } else {
            folder.setShareExpireAt(null);
        }

        folder.setUpdatedAt(new Date());
        folderRepository.save(folder);

        return token;
    }

    @Override
    public void revokeShareToken(String uid, Long folderId, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("目录不存在"));

        if (!folder.getOwnerId().equals(uid)) {
            throw new SecurityException("无权限取消分享");
        }

        folder.setShared(false);
        folder.setShareToken(null);
        folder.setUpdatedAt(new Date());
        folderRepository.save(folder);
    }

    @Override
    public Folder getFolderByShareToken(String token, String libraryCode) {
        return folderRepository.findByShareTokenAndLibraryCode(token, libraryCode).orElse(null);
    }


}
