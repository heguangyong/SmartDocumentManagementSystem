package com.github.sdms.repository;

import com.github.sdms.model.ShareAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareAccessLogRepository extends JpaRepository<ShareAccessLog, Long> {
}
