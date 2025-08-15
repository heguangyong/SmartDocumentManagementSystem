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
     *
     * @param uid         用户ID
     * @param libraryCode 租户代码（多租户支持）
     * @param path        请求路径
     * @return 时间验证结果
     */
    String logintimecheck(String uid, String libraryCode, String path);

    /**
     * 设置登录时间（暂时不改动）
     *
     * @param uid 用户ID
     */
    void loginset(String uid, String libraryCode);

    /**
     * 清理上传缓存（暂时不改动）
     *
     * @return 清理结果
     */
    boolean clearUploadCache();

    /**
     * 获取用户文件列表（暂时不改动）
     *
     * @param username 用户名
     * @return 用户文件列表
     */
    List<String> getUserFiles(String username);

    String uploadFile(Long userId, String bucketName, String objectName, MultipartFile file);

    /**
     * 上传文件
     *
     * @param userId      用户ID
     * @param file        上传的文件
     * @param libraryCode 租户代码（多租户支持）
     * @return 文件的名称或其他标识
     * @throws Exception 上传过程中抛出的异常
     */
    String uploadFile(Long userId, MultipartFile file, String libraryCode);

    /**
     * 根据文件名和桶名生成带签名的下载 URL
     *
     * @param userId      当前用户ID（用于权限校验）
     * @param libraryCode 当前馆点编码（用于权限校验，可选）
     * @param objectName  文件对象名（MinIO 中的 key）
     * @param bucketName  文件实际所在桶名
     * @return 带签名的下载 URL
     */
    String generatePresignedDownloadUrl(Long userId, String libraryCode, String objectName, String bucketName);

    /**
     * 获取公共下载链接
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 预签名的下载链接
     */
    String getPresignedUrl(String bucket, String objectName);

    /**
     * 获取文件内容
     *
     * @param bucket     存储桶名称
     * @param objectName 对象名称
     * @return 文件的输入流
     */
    InputStream getObject(String bucket, String objectName);

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    void deleteObject(String bucketName, String objectName);

    /**
     * 获取特定文件的下载链接
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象名称
     * @param filename  文件名
     * @return 预签名的下载链接
     */
    String getPresignedDownloadUrl(String bucket, String objectKey, String filename);

    /**
     * 从URL下载文件并上传的功能（辅助OnlyOffice回调）
     *
     * @param userId      用户ID
     * @param libraryCode 馆点码
     * @param docId       文档ID
     * @param fileUrl     文件URL
     * @return 带有时间戳的更新标记
     */
    String uploadFileFromUrl(Long userId, String libraryCode, Long docId, String fileUrl) throws Exception;

    /**
     * 根据用户ID和馆代码，获取对应桶的数据库主键ID
     */
    Long getBucketIdForUpload(Long userId, String libraryCode);


    long calculateUsedCapacity(String name);

    long getUsedCapacityWithCache(String bucketName); // 推荐添加

    void refreshBucketStatAsync(String bucketName);   // 如果外部需要触发，也可以加

    void copyObject(String bucket, String name, String bucket1, String newObjectName);

    /**
     * 上传文件 - 支持InputStream（用于OnlyOffice回调）
     * @param userId 用户ID
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名（用于推断Content-Type）
     * @return 实际上传的文件大小
     */
    long uploadFile(Long userId, String bucketName, String objectName,
                    InputStream inputStream, String originalFilename) throws Exception;

    long uploadFileWithTempFile(Long userId, String bucketName, String objectName,
                                InputStream inputStream, String originalFilename) throws Exception;
}
