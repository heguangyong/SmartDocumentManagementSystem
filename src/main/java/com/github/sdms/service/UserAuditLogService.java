package com.github.sdms.service;

import com.github.sdms.model.enums.AuditActionType;

public interface UserAuditLogService {
    void log(String userId, String username, String libraryCode, String ip, String userAgent, AuditActionType type, String detail);
}
