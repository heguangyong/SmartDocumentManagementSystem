package com.github.sdms.repository;

import com.github.sdms.model.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    boolean existsByUidAndPermissionTypeAndResourceId(String uid, String permissionType, Long resourceId);
}
