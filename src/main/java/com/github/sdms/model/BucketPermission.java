package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
@Entity
@Table(name = "bucket_permission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "bucket_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 将 uid 改为 userId，存储用户表主键ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bucket_id", nullable = false)
    private Long bucketId;

    /**
     * 权限字符串，例如 "read", "write", "read,write", "admin"
     */
    @Column(nullable = false, length = 64)
    private String permission;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", insertable = false, updatable = false)
    private Bucket bucket;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
