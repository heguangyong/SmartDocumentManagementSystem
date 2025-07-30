package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_permission")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 用户

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private UserFile file; // 文件

    @Column(nullable = false)
    private String permission; // 权限类型：READ, WRITE, DELETE
}
