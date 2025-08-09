package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "user_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 对应用户表主键 ID

    @Column(nullable = false)
    private String uid; // 上传用户 ID,来自于用户api接口同步数据

    @Column(nullable = false)
    private String name; // 用于存储 MinIO 的 objectName

    @Column(name = "origin_fn")
    private String originFilename;  // 用于保存上传文件的原始文件名

    private String type; // 文件类型

    private Long size; // 文件大小（字节）

    private String url; // MinIO 对象路径

    private String md5; // 文件MD5

    @Column(name = "bucket_id")
    private Long bucketId; //存储桶Id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", insertable = false, updatable = false)
    private Bucket bucketEntity;

    private String bucket; // 存储桶名 (一期遗留字段，暂时保留兼容，计划废弃)

    @Column(name = "delete_flag")
    private Boolean deleteFlag = false; // 是否已删除

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createdDate = new Date();// 对应上传时间

    private Integer uperr = 0; // 上传错误标志位

    private String ip; // 上传来源 IP（可选）

    @Column(name = "folder_id")
    private Long folderId; // 所属目录 ID，可为空，表示根目录

    // 新增馆代码字段
    @Column(nullable = false)
    private String libraryCode;

    @Column(name = "doc_id")
    private Long docId; // 所属文档ID

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "version_notes")
    private String notes;// 版本说明

    @Column(name = "is_latest")
    private Boolean isLatest = true;

    public String getVersionKey() {
        if (docId == null || versionNumber == null) {
            return null;
        }
        return docId + "_v" + versionNumber;
    }

    private Boolean shared; // 是否公开分享
    private String shareToken; // 唯一分享 token
    private Date shareExpireAt; // 过期时间

}