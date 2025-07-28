package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_permission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uid", "permission_type", "resource_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "permission_type", nullable = false)
    private String permissionType; // READ / WRITE / DELETE 等

    @Column(name = "resource_id", nullable = false)
    private Long resourceId; // 对应 permission_resource.id

    @Column(name = "created_time", nullable = false)
    private Long createdTime = System.currentTimeMillis();
}
