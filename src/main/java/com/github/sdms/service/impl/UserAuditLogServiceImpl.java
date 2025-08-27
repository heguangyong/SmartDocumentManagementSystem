package com.github.sdms.service.impl;

import com.github.sdms.dto.ThirdConfig;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.UserAuditLog;
import com.github.sdms.model.enums.AuditActionType;
import com.github.sdms.repository.UserAuditLogRepository;
import com.github.sdms.service.UserAuditLogService;
import com.github.sdms.util.KmsUtils;
import com.github.sdms.util.SignUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private final UserAuditLogRepository repository;

    private final ThirdConfig thirdConfig; // 注入三方配置（证书、IP、端口等）

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;

    @Value("${kms.enabled:false}")
    private boolean kmsEnabled;

    @Value("${sdms.crypto.mock-when-disabled:false}")
    private boolean mockWhenDisabled;

    @Value("${spring.verify:false}")
    private boolean verify;

    @Resource
    private SignUtil signUtil;  // 通过Spring注入，不再手动创建

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
            System.out.println("kmsEnabled=" + kmsEnabled + ", signatureEnabled=" + signatureEnabled);

            // === KMS 加密 ===
            if (kmsEnabled) {
                log.setIpEnc(KmsUtils.encrypt(log.getIp()));
                log.setUserAgentEnc(KmsUtils.encrypt(log.getUserAgent()));
            }

            // === SVS 签名 ===
            if (signatureEnabled) {
                String origin = buildSignatureDataForSign(log);
                // 生成签名前打印
                System.out.println("origin: " + origin);
                String signature = signUtil.getSignB64SignedDataV2(origin);  // 移除 helper 参数
                System.out.println("signature: " + signature);
                log.setSignature(signature);
                System.out.println("log.signature set: " + log.getSignature());

                // === 生成后立刻做一次验签 ===
                if (verify && signature != null) {
                    boolean ok = signUtil.tryValidateSignV2(origin, signature);  // 移除 helper 参数
                    System.out.println("验证结果: " + ok);
                    if (!ok) {
                        throw new ApiException(500, "签名验证失败，数据可能被篡改");
                    }
                }
            } else {
                log.setSignature(mockWhenDisabled ? "mock-signature-disabled" : null);
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
        return signUtil.validateSignB64SignedDataV2(origin, log.getSignature());  // 直接返回验签结果，移除 helper 相关代码
    }

    // buildSignatureDataForSign 方法保持不变
    private String buildSignatureDataForSign(UserAuditLog log) {
        return String.join("|",
                s(log.getUserId()),
                n(log.getUsername()),
                n(log.getLibraryCode()),
                n(log.getIpEnc()), // 用加密后的 IP
                n(log.getUserAgentEnc()), // 用加密后的 UserAgent
                n(log.getActionType()),
                n(log.getActionDetail()),
                d(log.getCreatedTime())
        );
    }

    private String n(String v){ return v==null?"":v; }
    private String s(Long v){ return v==null?"":String.valueOf(v); }
    private String d(Date dt){ return dt==null? new Date().toString(): dt.toString(); }
}

