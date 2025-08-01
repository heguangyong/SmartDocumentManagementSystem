package com.github.sdms.model;

import com.github.sdms.model.enums.PermissionType;
import com.github.sdms.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_permission", uniqueConstraints = @UniqueConstraint(columnNames = {"role", "resource_id", "permission"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  // 使用枚举类型，存储为字符串
    @Column(nullable = false)
    private RoleType roleType; // LIBRARIAN, ADMIN, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private PermissionResource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permission; // READ, WRITE, DELETE
}
