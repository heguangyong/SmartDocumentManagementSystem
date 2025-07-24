package com.github.sdms.repository;

import com.github.sdms.model.PermissionResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionResourceRepository extends JpaRepository<PermissionResource, Long> {
    Optional<PermissionResource> findByResourceTypeAndResourceKey(String resourceType, String resourceKey);

    boolean existsByResourceKey(String resourceKey);

    Optional<PermissionResource> findByResourceKey(String id);

}