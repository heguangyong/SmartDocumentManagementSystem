package com.github.sdms.service;

import com.github.sdms.model.UserAuditLog;
import com.github.sdms.model.enums.AuditActionType;

public interface UserAuditLogService {
    void log(Long userId, String username, String libraryCode, String ip, String userAgent, AuditActionType type, String detail);
    boolean verifyLogSignature(UserAuditLog log);
}
