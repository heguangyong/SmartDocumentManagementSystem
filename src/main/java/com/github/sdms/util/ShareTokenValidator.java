package com.github.sdms.util;

import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;

import java.util.Date;

public class ShareTokenValidator {

    /**
     * 校验分享 token 是否有效（是否共享 + 是否过期）
     * @param folder 通过 token 获取到的 Folder 对象
     * @throws IllegalStateException 若 token 无效或已过期
     */
    public static void validateShareToken(Folder folder) {
        if (folder == null || !Boolean.TRUE.equals(folder.getShared())) {
            throw new IllegalStateException("无效的分享链接");
        }

        if (folder.getShareExpireAt() != null && new Date().after(folder.getShareExpireAt())) {
            throw new IllegalStateException("分享链接已过期");
        }
    }

    public static void validateFileShareToken(UserFile userFile) {
        if (userFile == null || !Boolean.TRUE.equals(userFile.getShared())) {
            throw new IllegalStateException("无效的分享链接");
        }
        if (userFile.getShareExpireAt() != null && new Date().after(userFile.getShareExpireAt())) {
            throw new IllegalStateException("分享链接已过期");
        }
    }
}
