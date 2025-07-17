package com.github.sdms.util;

import com.github.sdms.model.Folder;
import com.github.sdms.model.UserFile;

public class ShareUrlUtil {
    public static String generateFileShareUrl(UserFile file) {
        return "https://yourhost/share/file/" + file.getShareToken();
    }
    public static String generateFolderShareUrl(Folder folder) {
        return "https://yourhost/share/folder/" + folder.getShareToken();
    }
}
