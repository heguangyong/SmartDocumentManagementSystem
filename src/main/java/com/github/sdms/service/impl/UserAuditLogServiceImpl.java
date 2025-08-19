package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.UserAuditLog;
import com.github.sdms.model.enums.AuditActionType;
import com.github.sdms.repository.UserAuditLogRepository;
import com.github.sdms.service.KmsCryptoService;
import com.github.sdms.service.SvsSignService;
import com.github.sdms.service.UserAuditLogService;
import com.koalii.svs.client.Svs2ClientHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private final UserAuditLogRepository repository;
    private final SvsSignService svsSignService;
    private final Optional<KmsCryptoService> kmsOpt;
    private final Optional<Svs2ClientHelper> helperOpt;

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;
    @Value("${kms.enabled:false}")
    private boolean kmsEnabled;

    @Transactional
    @Override
    public void log(Long userId, String username, String libraryCode, String ip, String userAgent,
                    AuditActionType actionType, String actionDetail) {
        UserAuditLog log = new UserAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLibraryCode(libraryCode);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setActionType(actionType.name());
        log.setActionDetail(actionDetail);
        log.setCreatedTime(new Date());

        try {
            if (kmsEnabled && kmsOpt.isPresent()) {
                KmsCryptoService kms = kmsOpt.get();
                log.setIp(kms.encryptToB64(log.getIp()));
                log.setUserAgent(kms.encryptToB64(log.getUserAgent()));
            }
            if (signatureEnabled) {
                String origin = buildSignatureDataForSign(log);
                log.setSignature(svsSignService.signB64(origin));
            } else {
                log.setSignature("mock-signature-disabled");
            }
            repository.save(log);
        } catch (Exception e) {
            throw new ApiException(500, "记录审计日志失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyLogSignature(UserAuditLog log) {
        if (!signatureEnabled) return true;
        String origin = buildSignatureDataForSign(log);
        return svsSignService.verify(origin, log.getSignature());
    }

    private String buildSignatureDataForSign(UserAuditLog log) {
        return String.join("|",
                s(log.getUserId()),
                n(log.getUsername()),
                n(log.getLibraryCode()),
                // 同上：签名最好用 ipHash/userAgentHash 代替原文
                n(log.getIp()),
                n(log.getUserAgent()),
                n(log.getActionType()),
                n(log.getActionDetail()),
                d(log.getCreatedTime())
        );
    }

    private String n(String v){ return v==null?"":v; }
    private String s(Long v){ return v==null?"":String.valueOf(v); }
    private String d(Date dt){ return dt==null? new Date().toString(): dt.toString(); }
}

