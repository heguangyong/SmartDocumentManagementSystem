package com.github.sdms.service.impl;

import com.github.sdms.model.*;
import com.github.sdms.repository.*;
import com.github.sdms.service.ShareAccessService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ShareAccessServiceImpl implements ShareAccessService {

    private final ShareAccessRepository shareAccessRepository;
    private final UserFileRepository userFileRepository;
    private final FolderRepository folderRepository;

    @Override
    public ShareAccess createFileShare(String uid, Long fileId, Integer expireMinutes, String libraryCode) {
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        if (!file.getUid().equals(uid)) {
            throw new SecurityException("无权限分享该文件");
        }

        ShareAccess share = buildBaseShare(uid, "file", fileId, expireMinutes, libraryCode);
        return shareAccessRepository.save(share);
    }

    @Override
    public ShareAccess createFolderShare(String uid, Long folderId, Integer expireMinutes, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("目录不存在"));

        if (!folder.getOwnerId().equals(uid)) {
            throw new SecurityException("无权限分享该目录");
        }

        ShareAccess share = buildBaseShare(uid, "folder", folderId, expireMinutes, libraryCode);
        return shareAccessRepository.save(share);
    }

    private ShareAccess buildBaseShare(String uid, String type, Long targetId, Integer expireMinutes, String libraryCode) {
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        ShareAccess share = new ShareAccess();
        share.setToken(token);
        share.setTokenHash(DigestUtils.sha256Hex(token));
        share.setType(type);
        share.setTargetId(targetId);
        share.setOwnerUid(uid);
        share.setLibraryCode(libraryCode);
        share.setCreatedAt(new Date());
        share.setActive(true);

        if (expireMinutes != null && expireMinutes > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, expireMinutes);
            share.setExpireAt(calendar.getTime());
        }

        return share;
    }

    @Override
    public void revokeShare(String uid, String token, String libraryCode) {
        ShareAccess share = shareAccessRepository.findByTokenAndLibraryCode(token, libraryCode)
                .orElseThrow(() -> new RuntimeException("分享链接不存在"));

        if (!share.getOwnerUid().equals(uid)) {
            throw new SecurityException("无权限撤销该分享");
        }

        shareAccessRepository.delete(share);
    }

    @Override
    public ShareAccess getByToken(String token, String libraryCode) {
        return shareAccessRepository.findByTokenAndLibraryCode(token, libraryCode)
                .orElseThrow(() -> new RuntimeException("无效的分享链接"));
    }

    @Override
    public UserFile getFileByToken(String token, String libraryCode) {
        ShareAccess share = getByToken(token, libraryCode);
        if (!"file".equals(share.getType())) {
            throw new RuntimeException("分享类型错误，非文件");
        }

        if (isShareExpired(share)) {
            throw new RuntimeException("分享链接已过期");
        }

        return userFileRepository.findById(share.getTargetId())
                .orElseThrow(() -> new RuntimeException("文件不存在"));
    }

    @Override
    public Folder getFolderByToken(String token, String libraryCode) {
        ShareAccess share = getByToken(token, libraryCode);
        if (!"folder".equals(share.getType())) {
            throw new RuntimeException("分享类型错误，非目录");
        }

        if (isShareExpired(share)) {
            throw new RuntimeException("分享链接已过期");
        }

        return folderRepository.findById(share.getTargetId())
                .orElseThrow(() -> new RuntimeException("目录不存在"));
    }

    @Override
    public boolean isShareExpired(ShareAccess access) {
        return access.getExpireAt() != null && access.getExpireAt().before(new Date());
    }

    @Override
    public List<Folder> listChildFolders(Folder folder) {
        return folderRepository.findByParentIdAndUidAndLibraryCode(
                folder.getId(), folder.getOwnerId(), folder.getLibraryCode()
        );
    }

    @Override
    public List<UserFile> listFilesByFolder(Folder folder) {
        return userFileRepository.findByFolderIdAndUidAndLibraryCode(
                folder.getId(), folder.getOwnerId(), folder.getLibraryCode()
        );
    }

    @Override
    public UserFile getFileByIdAndValidate(Folder folder, Long fileId) {
        return userFileRepository.findById(fileId)
                .filter(file -> file.getFolderId().equals(folder.getId()))
                .orElse(null);
    }

    @Override
    public String createShare(String uid, String type, Long targetId, Date expireAt) {
        int expireMinutes = (expireAt != null)
                ? (int) ((expireAt.getTime() - System.currentTimeMillis()) / 60000)
                : 0;

        ShareAccess share;
        if ("file".equalsIgnoreCase(type)) {
            share = createFileShare(uid, targetId, expireMinutes > 0 ? expireMinutes : null, null);
        } else if ("folder".equalsIgnoreCase(type)) {
            share = createFolderShare(uid, targetId, expireMinutes > 0 ? expireMinutes : null, null);
        } else {
            throw new IllegalArgumentException("不支持的分享类型: " + type);
        }

        return share.getToken();
    }
}
