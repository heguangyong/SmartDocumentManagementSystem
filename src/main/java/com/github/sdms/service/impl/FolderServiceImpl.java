package com.github.sdms.service.impl;

import com.github.sdms.dto.FolderPageRequest;
import com.github.sdms.dto.FolderSummaryDTO;
import com.github.sdms.dto.MoveRequest;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.FolderRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.FolderService;
import com.github.sdms.service.UserFileService;
import com.github.sdms.util.CustomerUserDetails;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserFileRepository fileRepository;

    @Autowired
    @Lazy
    private UserFileService userFileService;

    @Override
    public Page<FolderSummaryDTO> pageFolders(FolderPageRequest request, CustomerUserDetails userDetails) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<Folder> page = folderRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleteFlag"), false));

            switch (userDetails.getRoleType()) {
                case ADMIN:
                    break;
                default:
                    predicates.add(cb.equal(root.get("libraryCode"), userDetails.getLibraryCode()));
                    predicates.add(cb.equal(root.get("userId"), userDetails.getUserId()));
                    break;
            }

            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + request.getKeyword() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<FolderSummaryDTO> dtoList = page.getContent().stream().map(folder -> {
            FolderSummaryDTO dto = new FolderSummaryDTO();
            dto.setId(folder.getId());
            dto.setName(folder.getName());
            dto.setParentId(folder.getParentId());
            dto.setCreatedDate(folder.getCreatedDate());
            return dto;
        }).toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }


    @Override
    public Folder createFolder(Long userId, String name, Long parentId, Long bucketId, String libraryCode) {
        // 同一父目录下（同一桶/库）检查是否有同名文件夹
        List<Folder> siblings = (parentId == null)
                ? folderRepository.findByUserIdAndParentIdIsNullAndBucketIdAndLibraryCode(userId, bucketId, libraryCode)
                : folderRepository.findByUserIdAndParentIdAndBucketIdAndLibraryCode(userId, parentId, bucketId, libraryCode);

        if (siblings.stream().anyMatch(f -> f.getName().equals(name))) {
            throw new ApiException(400, "该目录下已存在同名文件夹");
        }

        Folder folder = Folder.builder()
                .userId(userId)
                .name(name)
                .parentId(parentId)
                .bucketId(bucketId)            // 绑定存储桶
                .libraryCode(libraryCode)
                .createdDate(new Date())
                .updatedDate(new Date())
                .build();

        return folderRepository.save(folder);
    }


    @Override
    public Folder renameFolder(Long userId, Long folderId, String newName, String libraryCode) {
        Folder folder = getFolderById(userId, folderId, libraryCode);

        List<Folder> siblings = (folder.getParentId() == null)
                ? folderRepository.findByUserIdAndParentIdIsNullAndLibraryCode(userId, libraryCode)
                : folderRepository.findByUserIdAndParentIdAndLibraryCode(userId, folder.getParentId(), libraryCode);

        if (siblings.stream().anyMatch(f -> !f.getId().equals(folderId) && f.getName().equals(newName))) {
            throw new ApiException(400, "该目录下已存在同名文件夹");
        }

        folder.setName(newName);
        folder.setUpdatedDate(new Date());
        return folderRepository.save(folder);
    }

    @Override
    public void deleteFolder(Long userId, Long folderId, String libraryCode) {
        Folder folder = getFolderById(userId, folderId, libraryCode);
        if (Boolean.TRUE.equals(folder.getSystemFolder())) {
            throw new ApiException(403, "系统内置目录禁止删除");
        }
        folderRepository.delete(folder);
    }

    @Override
    public List<Folder> listFolders(Long userId, Long parentId, String libraryCode) {
        return (parentId == null)
                ? folderRepository.findByUserIdAndParentIdIsNullAndLibraryCode(userId, libraryCode)
                : folderRepository.findByUserIdAndParentIdAndLibraryCode(userId, parentId, libraryCode);
    }

    @Override
    public List<Folder> listAllFolders(Long userId, String libraryCode) {
        return folderRepository.findByUserIdAndLibraryCode(userId, libraryCode);
    }

    @Override
    public Folder getFolderById(Long userId, Long folderId, String libraryCode) {
        Folder folder = folderRepository.findByIdAndUserIdAndLibraryCode(folderId, userId, libraryCode);
        if (folder == null) {
            throw new ApiException(404, "目录不存在或无权限访问");
        }
        return folder;
    }

    @Override
    public void moveFolder(Long userId, Long folderId, Long newParentId, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ApiException(404, "待移动目录不存在"));

        Folder newParent = folderRepository.findById(newParentId)
                .orElseThrow(() -> new ApiException(404, "目标父目录不存在"));

        if (!folder.getUserId().equals(userId) || !newParent.getUserId().equals(userId)) {
            throw new ApiException(403, "无权限操作该目录");
        }

        if (isDescendant(folderId, newParentId)) {
            throw new ApiException(400, "不能将目录移动到其子目录下");
        }

        folder.setParentId(newParentId);
        folder.setUpdatedDate(new Date());
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

    /**
     * 递归获取指定目录ID的所有子目录ID列表
     *
     * @param folderId 目录ID
     * @return 子目录ID列表，不包含自身
     */
    @Override
    public List<Long> getAllSubFolderIds(Long folderId) {
        List<Long> result = new ArrayList<>();
        collectSubFolderIds(folderId, result);
        return result;
    }

    private void collectSubFolderIds(Long parentId, List<Long> collector) {
        List<Folder> children = folderRepository.findByParentId(parentId);
        for (Folder child : children) {
            collector.add(child.getId());
            collectSubFolderIds(child.getId(), collector);
        }
    }

    /**
     * 根据父文件夹ID获取子文件夹列表
     */
    @Override
    public List<Folder> listFoldersByParentId(Long userId, Long parentId, String libraryCode) {
        return folderRepository.findByUserIdAndParentIdAndLibraryCode(userId, parentId, libraryCode);
    }

    /**
     * 获取桶下的根级文件夹（parentId 为 null）
     */
    @Override
    public List<Folder> listRootFoldersByBucket(Long userId, Long bucketId, String libraryCode) {
        return folderRepository.findByUserIdAndBucketIdAndParentIdIsNullAndLibraryCode(
                userId, bucketId, libraryCode);
    }

    /**
     * 获取桶下所有文件夹
     */
    @Override
    public List<Folder> listAllFoldersByBucket(Long userId, Long bucketId, String libraryCode) {
        return folderRepository.findByUserIdAndBucketIdAndLibraryCode(
                userId, bucketId, libraryCode);
    }

    /**
     * 获取指定文件夹的所有后代文件夹ID
     */
    @Override
    public Set<Long> getAllDescendantIds(Long folderId, List<Folder> allFolders) {
        Set<Long> descendants = new HashSet<>();
        Map<Long, List<Folder>> parentChildMap = allFolders.stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(Folder::getParentId));

        collectDescendants(folderId, parentChildMap, descendants);
        return descendants;
    }

    private void collectDescendants(Long parentId, Map<Long, List<Folder>> parentChildMap, Set<Long> descendants) {
        List<Folder> children = parentChildMap.get(parentId);
        if (children != null) {
            for (Folder child : children) {
                descendants.add(child.getId());
                collectDescendants(child.getId(), parentChildMap, descendants);
            }
        }
    }

    @Override
    @Transactional
    public void moveBatch(Long userId, MoveRequest moveRequest, String libraryCode) {
        Long targetFolderId = moveRequest.getTargetFolderId();

        // 验证目标文件夹（如果不是移动到根目录）
        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findById(targetFolderId)
                    .orElseThrow(() -> new ApiException(404, "目标父目录不存在"));
            if (!targetFolder.getUserId().equals(userId)) {
                throw new ApiException(403, "无权限操作该目录");
            }
        }

        // 批量移动文件夹
        List<Long> folderIds = moveRequest.getFolderIds();
        if (folderIds != null && !folderIds.isEmpty()) {
            moveFolders(userId, folderIds, targetFolderId, targetFolder);
        }

        // 批量移动文件
        List<Long> fileIds = moveRequest.getFileIds();
        if (fileIds != null && !fileIds.isEmpty()) {
            moveFiles(userId, fileIds, targetFolderId);
        }
    }

    private void moveFolders(Long userId, List<Long> folderIds, Long targetFolderId, Folder targetFolder) {
        for (Long folderId : folderIds) {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ApiException(404, "待移动目录不存在"));

            // 权限检查：检查源文件夹权限
            if (!folder.getUserId().equals(userId)) {
                throw new ApiException(403, "无权限操作该目录");
            }

            // 检查是否移动到子目录（只有当targetFolderId不为null时才需要检查）
            if (targetFolderId != null && isDescendant(folderId, targetFolderId)) {
                throw new ApiException(400, "不能将目录移动到其子目录下");
            }

            // 设置新的父目录ID，null表示移动到根目录
            folder.setParentId(targetFolderId);
            folder.setUpdatedDate(new Date());
            folderRepository.save(folder);
        }
    }

    private void moveFiles(Long userId, List<Long> fileIds, Long targetFolderId) {
        for (Long fileId : fileIds) {
            UserFile file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ApiException(404, "待移动文件不存在"));

            // 权限检查
            if (!file.getUserId().equals(userId)) {
                throw new ApiException(403, "无权限操作该文件");
            }

            // 设置新的文件夹ID，null表示移动到根目录
            file.setFolderId(targetFolderId);
            file.setUpdateTime(LocalDateTime.now());
            fileRepository.save(file);
        }
    }

    @Transactional
    public void deleteFolderWithFiles(Long userId, Long folderId, String libraryCode) {
        // 获取文件夹下所有文件
        List<UserFile> files = userFileService.listFilesByFolder(userId, folderId, libraryCode);
        for (UserFile file : files) {
            file.setDeleteFlag(true);
            userFileService.saveUserFile(file);

            // 如果删除的是最新版本，更新剩余版本
            if (Boolean.TRUE.equals(file.getIsLatest())) {
                UserFile latestRemaining = userFileService.getHighestVersionFile(file.getDocId(), userId, libraryCode);
                if (latestRemaining != null) {
                    latestRemaining.setIsLatest(true);
                    userFileService.saveUserFile(latestRemaining);
                }
            }
        }

        // 删除文件夹
        Folder folder = folderRepository.findByIdAndLibraryCode(folderId, libraryCode)
                .orElseThrow(() -> new ApiException(404, "文件夹不存在"));
        folderRepository.delete(folder);
    }

}
