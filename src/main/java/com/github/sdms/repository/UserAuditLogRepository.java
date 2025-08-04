package com.github.sdms.repository;

import com.github.sdms.model.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
    List<UserAuditLog> findByUserId(String userId);
}

