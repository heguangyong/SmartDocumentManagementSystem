package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.Folder;
import com.github.sdms.model.ShareAccess;
import com.github.sdms.model.UserFile;
import com.github.sdms.repository.FolderRepository;
import com.github.sdms.repository.ShareAccessRepository;
import com.github.sdms.repository.UserFileRepository;
import com.github.sdms.service.ShareAccessService;
import com.github.sdms.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareAccessServiceImpl implements ShareAccessService {

    private final ShareAccessRepository shareAccessRepository;
    private final UserFileRepository userFileRepository;
    private final FolderRepository folderRepository;

    @Override
    public ShareAccess createFileShare(Long userId, Long fileId, Integer expireMinutes, String libraryCode) {
        UserFile file = userFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(404, "文件不存在"));
        if (!file.getUserId().equals(userId)) {
            throw new ApiException(403, "无权限分享该文件");
        }

        ShareAccess share = buildBaseShare(userId, "file", fileId, file.getOriginFilename(), expireMinutes, libraryCode);
        return shareAccessRepository.save(share);
    }

    @Override
    public ShareAccess createFolderShare(Long userId, Long folderId, Integer expireMinutes, String libraryCode) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ApiException(404, "目录不存在"));
        if (!folder.getOwnerId().equals(userId)) {
            throw new ApiException(403, "无权限分享该目录");
        }

        ShareAccess share = buildBaseShare(userId, "folder", folderId, folder.getName(), expireMinutes, libraryCode);
        return shareAccessRepository.save(share);
    }

    @Override
    public String createShare(Long userId, String targetType, Long targetId, Date expireAt, String libraryCode) {
        Integer expireMinutes = (expireAt != null)
                ? (int) ((expireAt.getTime() - System.currentTimeMillis()) / 60000)
                : null;

        ShareAccess share;
        if ("file".equalsIgnoreCase(targetType)) {
            share = createFileShare(userId, targetId, expireMinutes, libraryCode);
        } else if ("folder".equalsIgnoreCase(targetType)) {
            share = createFolderShare(userId, targetId, expireMinutes, libraryCode);
        } else {
            throw new ApiException(400, "不支持的 targetType：" + targetType);
        }
        return share.getToken(); // 明文 token 返回前端
    }

    private ShareAccess buildBaseShare(Long userId, String type, Long targetId, String targetName, Integer expireMinutes, String libraryCode) {
        String token = TokenUtils.generateToken();
        String hash = TokenUtils.hashToken(token);

        ShareAccess share = ShareAccess.builder()
                .token(token)
                .tokenHash(hash)
                .targetType(type)
                .targetId(targetId)
                .targetName(targetName)
                .ownerId(userId)
                .libraryCode(libraryCode)
                .createdAt(new Date())
                .enabled(true)
                .build();

        if (expireMinutes != null && expireMinutes > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, expireMinutes);
            share.setExpireAt(cal.getTime());
        }
        return share;
    }

    @Override
    public void revokeShare(Long userId, String token) {
        ShareAccess share = getRawByToken(token);
        if (!share.getOwnerId().equals(userId) ) {
            throw new ApiException(403, "无权限撤销该分享");
        }
        share.setEnabled(false); // 逻辑禁用
        shareAccessRepository.save(share);
    }

    @Override
    public ShareAccess getByToken(String token) {
        ShareAccess share = getRawByToken(token);
        if (!Boolean.TRUE.equals(share.getEnabled())) {
            throw new ApiException(403, "该分享已被禁用");
        }
        if (isShareExpired(share)) {
            throw new ApiException(403, "该分享链接已过期");
        }
        return share;
    }

    @Override
    public ShareAccess getRawByToken(String token) {
        String hash = TokenUtils.hashToken(token);
        return shareAccessRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(404, "无效的分享链接"));
    }

    @Override
    public UserFile getFileByToken(String token) {
        ShareAccess share = getByToken(token);
        if (!"file".equalsIgnoreCase(share.getTargetType())) {
            throw new ApiException(400, "分享类型错误：不是文件类型");
        }
        return userFileRepository.findById(share.getTargetId())
                .orElseThrow(() -> new ApiException(404, "文件不存在"));
    }

    @Override
    public Folder getFolderByToken(String token) {
        ShareAccess share = getByToken(token);
        if (!"folder".equalsIgnoreCase(share.getTargetType())) {
            throw new ApiException(400, "分享类型错误：不是目录类型");
        }
        return folderRepository.findById(share.getTargetId())
                .orElseThrow(() -> new ApiException(404, "目录不存在"));
    }

    @Override
    public boolean isShareExpired(ShareAccess access) {
        return access.getExpireAt() != null && access.getExpireAt().before(new Date());
    }

    @Override
    public List<UserFile> listFilesByFolder(Folder folder) {
        return userFileRepository.findByFolderIdAndUserIdAndLibraryCode(
                folder.getId(), folder.getOwnerId(), folder.getLibraryCode());
    }

    @Override
    public List<Folder> listChildFolders(Folder folder) {
        return folderRepository.findByParentIdAndUserIdAndLibraryCode(
                folder.getId(), folder.getOwnerId(), folder.getLibraryCode());
    }

    @Override
    public UserFile getFileByIdAndValidate(Folder folder, Long fileId) {
        return userFileRepository.findById(fileId)
                .filter(file -> file.getFolderId().equals(folder.getId()))
                .orElse(null);
    }

    @Override
    public List<ShareAccess> listMyShares(Long userId, String targetType, String libraryCode) {
        return shareAccessRepository.findByOwnerIdAndTargetTypeAndLibraryCodeAndEnabledTrue(
                userId, targetType, libraryCode);
    }
}
