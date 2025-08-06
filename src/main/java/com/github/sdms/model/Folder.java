package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "folder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 目录名
    @Column(nullable = false)
    private String name;

    // 新增馆代码字段
    @Column(nullable = false)
    private String libraryCode;

    // 所属用户 ID（私有目录用）
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 父目录 ID，根目录为 null
    private Long parentId;

    // 是否公开共享（可扩展共享策略）
    private Boolean isPublic = false;

    // 是否系统内置目录（如默认上传目录）
    private Boolean systemFolder = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate = new Date();

    @Column(name = "shared")
    private Boolean shared = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "share_expire_at")
    private Date shareExpireAt; // 分享链接过期时间（可空 = 永不过期）

    public Long getOwnerId() {
        return this.userId;
    }

}
