package com.github.sdms.service.impl;

import com.github.sdms.model.UserAuditLog;
import com.github.sdms.model.enums.AuditActionType;
import com.github.sdms.repository.UserAuditLogRepository;
import com.github.sdms.service.UserAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private final UserAuditLogRepository logRepository;

    @Override
    public void log(String userId, String username, String libraryCode, String ip, String userAgent, AuditActionType type, String detail) {
        UserAuditLog log = UserAuditLog.builder()
                .userId(userId)
                .username(username)
                .libraryCode(libraryCode)
                .ip(ip)
                .userAgent(userAgent)
                .actionType(type.name())
                .actionDetail(detail)
                .createdTime(new Date())
                .ip(ip)
                .userAgent(userAgent)
                .build();
        logRepository.save(log);
    }
}
