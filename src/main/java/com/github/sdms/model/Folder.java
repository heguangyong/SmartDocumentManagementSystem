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

    // 所属用户 ID（私有目录用）
    @Column(nullable = false)
    private String uid;

    // 父目录 ID，根目录为 null
    private Long parentId;

    // 是否公开共享（可扩展共享策略）
    private Boolean isPublic = false;

    // 是否系统内置目录（如默认上传目录）
    private Boolean systemFolder = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();

    @Column(name = "shared")
    private Boolean shared = false;

    @Column(name = "share_token")
    private String shareToken;

    public String getOwnerId() {
        return this.uid;
    }

}
