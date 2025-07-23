package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bucket_permission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uid", "bucket_id"})
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

    @Column(nullable = false)
    private String uid;

    @Column(name = "bucket_id", nullable = false)
    private String bucketId;

    @Column(name = "can_read", nullable = false)
    private boolean canRead;

    @Column(name = "can_write", nullable = false)
    private boolean canWrite;
}
