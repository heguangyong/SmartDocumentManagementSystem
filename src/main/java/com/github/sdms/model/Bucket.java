package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "bucket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;  // 关联User表的主键ID

    // 新增馆代码字段
    @Column(nullable = true)
    private String libraryCode;

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();

    @Column(name = "max_capacity")
    private Long maxCapacity; // 单位：Byte
}

