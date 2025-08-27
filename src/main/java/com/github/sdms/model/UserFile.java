package com.github.sdms.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
    private String uid; // 上传用户 ID, 来自于用户API接口同步数据

    @Column(nullable = false, length = 512)
    private String name; // 用于存储 MinIO 的 objectName

    @Column(name = "origin_fn", length = 512)
    private String originFilename;  // 用于保存上传文件的原始文件名

    @Column(length = 255)
    private String type; // 文件类型

    @Column(length = 255)
    private String typename; // 文件类型

    private Long size; // 文件大小（字节）

    @Column(length = 2000)
    private String url; // MinIO 对象路径

    @Column(length = 500)
    private String md5; // 文件MD5

    @Column(name = "bucket_id")
    private Long bucketId; // 存储桶Id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", insertable = false, updatable = false)
    private Bucket bucketEntity;

    @Column(length = 500)
    private String bucket; // 存储桶名 (一期遗留字段，暂时保留兼容，计划废弃)

    @Column(name = "delete_flag")
    private Boolean deleteFlag = false; // 是否已删除

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", columnDefinition = "datetime(6)")
    private Date createdDate = new Date(); // 对应上传时间

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'")
    private LocalDateTime updateTime;


    private Integer uperr = 0; // 上传错误标志位

    @Schema(description = "来源ip")
    @Column(name = "ip",length = 500)
    private String ip; // 上传来源 IP（可选）

    @Column(name = "folder_id")
    private Long folderId; // 所属目录 ID，可为空，表示根目录

    // 新增馆代码字段
    @Column(nullable = false, length = 500)
    private String libraryCode;


    /**
     * 文档组ID - 同一文档的不同版本共享相同docId
     */
    @Column(name = "doc_id")
    private Long docId;

    /**
     * 业务版本号 - 同一docId下递增，用户可见的版本标识
     */
    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "version_notes", length = 512)
    private String notes; // 版本说明

    @Column(name = "is_latest", columnDefinition = "bit(1)")
    private Boolean isLatest = true; // 是否为最新版本

    public String getVersionKey() {
        if (docId == null || versionNumber == null) {
            return null;
        }
        return docId + "_v" + versionNumber;
    }

    /**
     * OnlyOffice版本Key - 用于OnlyOffice缓存控制，文档内容变更时需要更新
     * 格式建议：docId_versionNumber_timestamp 或 UUID
     */
    @Column(name = "version_key", length = 64)
    private String versionKey;

    /**
     * 生成OnlyOffice版本Key
     * 规则：docId_versionNumber_updateTime
     */
    public void generateVersionKey() {
        if (this.docId != null && this.versionNumber != null) {
            long timestamp = this.updateTime != null
                    ? Timestamp.valueOf(this.updateTime).getTime()
                    : System.currentTimeMillis();

            this.versionKey = String.format("%d_%d_%d", this.docId, this.versionNumber, timestamp);
        }
    }

    /**
     * 获取OnlyOffice版本Key，如果为空则生成
     */
    public String getOrGenerateVersionKey() {
        if (this.versionKey == null || this.versionKey.isEmpty()) {
            generateVersionKey();
        }
        return this.versionKey;
    }

    @Column(name = "shared", columnDefinition = "bit(1)")
    private Boolean shared; // 是否公开分享

    @Column(name = "share_token", length = 512)
    private String shareToken; // 唯一分享 token

    @Column(name = "share_expire_at", columnDefinition = "datetime(6)")
    private Date shareExpireAt; // 过期时间
}
