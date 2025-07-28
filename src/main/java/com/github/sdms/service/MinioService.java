package com.github.sdms.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface MinioService {

    /**
     * 验证签名（暂时不改动）
     */
    String urltoken(Map<String, Object> params);

    /**
     * 登录时间检查（暂时不改动）
     * @param uid 用户ID
     * @param libraryCode 租户代码（多租户支持）
     * @param path 请求路径
     * @return 时间验证结果
     */
    String logintimecheck(String uid, String libraryCode, String path);

    /**
     * 设置登录时间（暂时不改动）
     * @param uid 用户ID
     */
    void loginset(String uid, String libraryCode);

    /**
     * 清理上传缓存（暂时不改动）
     * @return 清理结果
     */
    boolean clearUploadCache();

    /**
     * 获取用户文件列表（暂时不改动）
     * @param username 用户名
     * @return 用户文件列表
     */
    List<String> getUserFiles(String username);

    String uploadFile(String bucketName, String uid, MultipartFile file);

    /**
     * 上传文件
     * @param uid 用户ID
     * @param file 上传的文件
     * @param libraryCode 租户代码（多租户支持）
     * @return 文件的名称或其他标识
     * @throws Exception 上传过程中抛出的异常
     */
    String uploadFile(String uid, MultipartFile file, String libraryCode) throws Exception;

    /**
     * 生成下载链接
     * @param uid 用户ID
     * @param libraryCode 租户代码（多租户支持）
     * @param objectName 文件名
     * @return 预签名的下载链接
     * @throws Exception 生成链接过程中抛出的异常
     */
    String generatePresignedDownloadUrl(String uid, String libraryCode, String objectName) throws Exception;

    /**
     * 获取公共下载链接
     * @param bucket 存储桶名称
     * @param objectName 对象名称
     * @return 预签名的下载链接
     */
    String getPresignedUrl(String bucket, String objectName);

    /**
     * 获取文件内容
     * @param bucket 存储桶名称
     * @param objectName 对象名称
     * @return 文件的输入流
     */
    InputStream getObject(String bucket, String objectName);

    /**
     * 删除文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    void deleteObject(String bucketName, String objectName);

    /**
     * 获取特定文件的下载链接
     * @param bucket 存储桶名称
     * @param objectKey 对象名称
     * @param filename 文件名
     * @return 预签名的下载链接
     */
    String getPresignedDownloadUrl(String bucket, String objectKey, String filename);

    /**
     * 从URL下载文件并上传的功能（辅助OnlyOffice回调）
     * @param uid 用户ID
     * @param libraryCode 馆点码
     * @param docId 文档ID
     * @param fileUrl 文件URL
     * @return 带有时间戳的更新标记
     */
    String uploadFileFromUrl(String uid, String libraryCode, Long docId, String fileUrl)  throws Exception;

    /**
     * 根据用户ID和馆代码，获取对应桶的数据库主键ID
     */
    Long getBucketIdForUpload(String uid, String libraryCode);


}
