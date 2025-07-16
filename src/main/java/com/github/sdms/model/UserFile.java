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

    @Column(nullable = false)
    private String uid; // 上传用户 ID

    @Column(nullable = false)
    private String name; // 存储名（唯一）

    @Column(name = "origin_fn")
    private String originFilename; // 原始文件名

    private String type; // 文件类型

    private Long size; // 文件大小（字节）

    private String url; // MinIO 对象路径

    private String md5; // 文件MD5

    private String bucket; // 存储桶名

    @Column(name = "delete_flag")
    private Boolean deleteFlag = false; // 是否已删除

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createdDate = new Date();

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
    private String notes;

    @Column(name = "is_latest")
    private Boolean isLatest = true;

    public String getVersionKey() {
        if (docId == null || versionNumber == null) {
            return null;
        }
        return docId + "_v" + versionNumber;
    }

}