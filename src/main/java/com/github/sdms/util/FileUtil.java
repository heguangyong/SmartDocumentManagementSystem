package com.github.sdms.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileUtil {

    /**
     * 从对象名中解析出文档ID，假设格式为 "docId_xxxx_yyyy.ext"
     * @param objectKey MinIO 对象名
     * @return docId 或 null（解析失败）
     */
    public static Long parseDocIdFromKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return null;
        try {
            // 取第一个下划线之前的部分作为docId字符串
            int underscoreIndex = objectKey.indexOf('_');
            if (underscoreIndex == -1) return null;
            String docIdStr = objectKey.substring(0, underscoreIndex);
            return Long.parseLong(docIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将远程文件URL转换为MultipartFile对象
     * @param fileUrl 远程文件地址
     * @return MultipartFile
     * @throws Exception 下载或转换失败抛异常
     */
    public static MultipartFile convertUrlToMultipartFile(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (InputStream inputStream = conn.getInputStream()) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            String contentType = conn.getContentType();

            return new MultipartFileImpl("file", fileName, contentType, inputStream);
        }
    }

}
