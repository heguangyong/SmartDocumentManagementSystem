package com.github.sdms.model;

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

    @Column(nullable = false)
    private String role; // LIBRARIAN, ADMIN, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private PermissionResource resource;

    @Column(nullable = false)
    private String permission; // READ, WRITE, DELETE
}
