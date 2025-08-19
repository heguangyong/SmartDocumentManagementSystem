package com.github.sdms.service.impl;

import com.github.sdms.model.UserAuditLog;
import com.github.sdms.model.enums.AuditActionType;
import com.github.sdms.repository.UserAuditLogRepository;
import com.github.sdms.service.UserAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private final UserAuditLogRepository repository;
//    private final Optional<Svs2ClientHelper> helperOpt;

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;

    @Override
    @Transactional
    public void log(Long userId, String username, String libraryCode, String ip, String userAgent, AuditActionType actionType, String actionDetail) {
//        UserAuditLog log = new UserAuditLog();
//        log.setUserId(userId);
//        log.setUsername(username);
//        log.setLibraryCode(libraryCode);
//        log.setIp(ip);
//        log.setUserAgent(userAgent);
//        log.setActionType(actionType.name());
//        log.setActionDetail(actionDetail);
//        log.setCreatedTime(new Date());
//
//        try {
//            if (signatureEnabled && helperOpt.isPresent()) {
//                Svs2ClientHelper helper = helperOpt.get();
//                String data = buildSignatureData(log);
//                String b64Data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
//
//                Svs2ClientHelper.SvsResultData result = helper.cdbPkcs7DetachSignEx(b64Data.getBytes(StandardCharsets.UTF_8), "sm2");
//                if (result.m_errno != 0 || result.m_b64SignedData == null) {
//                    throw new ApiException(500, "签名失败：错误码 " + result.m_errno);
//                }
//                log.setSignature(result.m_b64SignedData);
//            } else {
//                log.setSignature("mock-signature-disabled");
//            }
//            repository.save(log);
//        } catch (Exception e) {
//            throw new ApiException(500, "记录审计日志失败: " + e.getMessage());
//        }
    }

    @Override
    public boolean verifyLogSignature(UserAuditLog log) {
//        if (!signatureEnabled || helperOpt.isEmpty()) {
            return true;
//        }
//        try {
//            Svs2ClientHelper helper = helperOpt.get();
//            String data = buildSignatureData(log);
//            String b64Data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
//            Svs2ClientHelper.SvsResultData verifyResult = helper.cdbPkcs7DetachVerifyEx(log.getSignature(), b64Data.getBytes(StandardCharsets.UTF_8));
//            return verifyResult.m_errno == 0;
//        } catch (Exception e) {
//            throw new ApiException(500, "验签失败: " + e.getMessage());
//        }
    }

    private String buildSignatureData(UserAuditLog log) {
        return String.join("|",
                safe(log.getUserId() == null ? "" : log.getUserId().toString()),
                safe(log.getUsername()),
                safe(log.getLibraryCode()),
                safe(log.getIp()),
                safe(log.getUserAgent()),
                safe(log.getActionType()),
                safe(log.getActionDetail()),
                log.getCreatedTime() != null ? log.getCreatedTime().toString() : new Date().toString()
        );
    }

    private String safe(String input) {
        return input == null ? "" : input;
    }
}

