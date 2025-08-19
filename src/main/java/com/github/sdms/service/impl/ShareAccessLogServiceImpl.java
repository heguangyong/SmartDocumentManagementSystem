package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.repository.ShareAccessLogRepository;
import com.github.sdms.service.KmsCryptoService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.service.SvsSignService;
import com.koalii.svs.client.Svs2ClientHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShareAccessLogServiceImpl implements ShareAccessLogService {

    private final ShareAccessLogRepository repository;
    private final SvsSignService svsSignService;
    private final Optional<KmsCryptoService> kmsOpt;
    private final Optional<Svs2ClientHelper> helperOpt;

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;

    @Value("${kms.enabled:false}")
    private boolean kmsEnabled;

    @Value("${sdms.crypto.mock-when-disabled:false}")
    private boolean mockWhenDisabled;

    @Override
    @Transactional
    public void recordAccess(ShareAccessLog log) {
        try {
            // 1. 计算敏感字段摘要用于签名（不使用明文）
            String tokenHashDigest = hashHex(log.getToken() == null ? "" : log.getToken());
            String ipDigest = hashHex(log.getAccessIp() == null ? "" : log.getAccessIp());
            String uaDigest = hashHex(log.getUserAgent() == null ? "" : log.getUserAgent());

            // 2. 对敏感字段加密入库（若开启KMS）
            if (kmsEnabled && kmsOpt.isPresent()) {
                KmsCryptoService kms = kmsOpt.get();
                log.setTokenHash(kms.encryptToB64(tokenHashDigest)); // 存摘要密文
                log.setAccessIp(kms.encryptToB64(log.getAccessIp()));
                log.setUserAgent(kms.encryptToB64(log.getUserAgent()));
            } else {
                log.setTokenHash(tokenHashDigest);
            }

            // 3. 签名：使用非敏感字段 + 敏感字段的摘要（明文摘要）
            String origin = String.join("|",
                    n(log.getAccessIp()), // 可使用摘要替代，这里示例使用摘要串
                    log.getAccessTime() != null ? log.getAccessTime().toString() : new Date().toString(),
                    String.valueOf(log.getFileId()),
                    n(log.getActionType()),
                    tokenHashDigest
            );

            if (signatureEnabled) {
                String sig = svsSignService.signB64(origin);
                log.setSignature(sig);
            } else {
                log.setSignature(mockWhenDisabled ? "mock-signature-disabled" : null);
            }

            repository.save(log);
        } catch (Exception e) {
            throw new ApiException(500, "记录访问日志失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyAccessLogSignature(ShareAccessLog log) {
        if (!signatureEnabled) return true;
        try {
            // 如果入库时 tokenHash 被加密，需先解密后取摘要比较；这里假定 tokenHash 存的是摘要密文或摘要
            String tokenHashDigest;
            if (kmsEnabled && kmsOpt.isPresent()) {
                tokenHashDigest = kmsOpt.get().decryptFromB64(log.getTokenHash());
            } else {
                tokenHashDigest = log.getTokenHash();
            }

            String origin = String.join("|",
                    n(log.getAccessIp()),
                    log.getAccessTime() != null ? log.getAccessTime().toString() : new Date().toString(),
                    String.valueOf(log.getFileId()),
                    n(log.getActionType()),
                    tokenHashDigest
            );

            return svsSignService.verify(origin, log.getSignature());
        } catch (Exception e) {
            throw new ApiException(500, "验签失败: " + e.getMessage());
        }
    }

    private String hashHex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String n(String v){ return v==null?"":v; }

    @Override
    public List<ShareAccessLog> getAllLogs() {
        try {
            return repository.findAll(Sort.by(Sort.Direction.DESC, "accessTime"));
        } catch (Exception e) {
            throw new ApiException(500, "查询访问日志失败");
        }
    }

    private String buildSignatureData(ShareAccessLog log) {
        return String.join("|",
                safe(log.getAccessIp()),
                log.getAccessTime() != null ? log.getAccessTime().toString() : new Date().toString(),
                String.valueOf(log.getFileId()),
                safe(log.getActionType()),
                safe(log.getTokenHash())
        );
    }

    private String safe(String input) {
        return input == null ? "" : input;
    }
}
