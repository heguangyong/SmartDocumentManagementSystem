package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permission_resource")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "resource_type", nullable = false)
    private String resourceType; // BUCKET, FILE, etc.

    @Column(name = "resource_key", nullable = false)
    private String resourceKey; // bucketId 或路径
}
