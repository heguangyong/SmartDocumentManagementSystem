package com.github.sdms.service;

import com.github.sdms.model.ShareAccess;
import com.github.sdms.model.UserFile;
import com.github.sdms.model.Folder;

import java.util.Date;
import java.util.List;

public interface ShareAccessService {

    ShareAccess createFileShare(String uid, Long fileId, Integer expireMinutes, String libraryCode);

    ShareAccess createFolderShare(String uid, Long folderId, Integer expireMinutes, String libraryCode);

    void revokeShare(String uid, String token, String libraryCode);

    ShareAccess getByToken(String token, String libraryCode);

    UserFile getFileByToken(String token, String libraryCode);

    Folder getFolderByToken(String token, String libraryCode);

    boolean isShareExpired(ShareAccess access);

    List<Folder> listChildFolders(Folder folder);

    List<UserFile> listFilesByFolder(Folder folder);

    UserFile getFileByIdAndValidate(Folder folder, Long fileId);

    String createShare(String uid, String type, Long targetId, Date expireAt);
}
