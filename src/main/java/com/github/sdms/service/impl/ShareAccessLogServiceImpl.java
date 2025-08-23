package com.github.sdms.service.impl;

import com.github.sdms.dto.ThirdConfig;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.repository.ShareAccessLogRepository;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.util.KmsUtils;
import com.github.sdms.util.SignUtil;
import com.koalii.svs.client.Svs2ClientHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareAccessLogServiceImpl implements ShareAccessLogService {

    private final ShareAccessLogRepository repository;

    private final ThirdConfig thirdConfig; // 注入三方配置（证书、IP、端口等）

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;

    @Value("${kms.enabled:false}")
    private boolean kmsEnabled;

    @Value("${sdms.crypto.mock-when-disabled:false}")
    private boolean mockWhenDisabled;

    @Value("${app.verify:false}")
    private boolean verify;

    @Override
    @Transactional
    public void recordAccess(ShareAccessLog log) {
        log.setAccessTime(log.getAccessTime() == null ? new Date() : log.getAccessTime());

        try {
            System.out.println("kmsEnabled=" + kmsEnabled + ", signatureEnabled=" + signatureEnabled);

            // === KMS 加密 ===
            if (kmsEnabled) {
                log.setAccessIp(KmsUtils.encrypt(log.getAccessIp()));
                log.setUserAgent(KmsUtils.encrypt(log.getUserAgent()));
                log.setTokenHash(KmsUtils.encrypt(hashHex(log.getToken() == null ? "" : log.getToken())));
            } else {
                log.setTokenHash(hashHex(log.getToken() == null ? "" : log.getToken()));
            }

            // === SVS 签名 ===
            if (signatureEnabled) {
                String origin = buildSignatureData(log);
                // 生成签名前打印
                System.out.println("origin: " + origin);
                SignUtil signUtil = new SignUtil(thirdConfig);
                Svs2ClientHelper helper = signUtil.init();
                String signature = signUtil.getSignB64SignedData(origin, helper);
                System.out.println("signature: " + signature);
                log.setSignature(signature);
                System.out.println("log.signature set: " + log.getSignature());

                // === 生成后立刻做一次验签 ===
                if (verify && signature != null) {
                    boolean ok = signUtil.tryValidateSign(origin, helper, signature);
                    System.out.println("验证结果: " + ok);
                    if (!ok) {
                        throw new ApiException(500, "签名验证失败，数据可能被篡改");
                    }
                }

                signUtil.close(helper);
            } else {
                log.setSignature(mockWhenDisabled ? "mock-signature-disabled" : null);
            }
            // 保存前打印整个对象
            System.out.println("log before save: " + log);
            repository.save(log);
        } catch (Exception e) {
            throw new ApiException(500, "记录访问日志失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyAccessLogSignature(ShareAccessLog log) {
        if (!signatureEnabled) return true;

        try {
            String tokenDigest;
            if (kmsEnabled) {
                tokenDigest = KmsUtils.decrypt(log.getTokenHash());
            } else {
                tokenDigest = log.getTokenHash();
            }

            String origin = String.join("|",
                    n(log.getAccessIp()),
                    log.getAccessTime() != null ? log.getAccessTime().toString() : new Date().toString(),
                    String.valueOf(log.getFileId()),
                    n(log.getActionType()),
                    tokenDigest
            );

            SignUtil signUtil = new SignUtil(thirdConfig);
            Svs2ClientHelper helper = signUtil.init();
            signUtil.validateSignB64SignedData(origin, helper, log.getSignature());
            signUtil.close(helper);

            return true;
        } catch (Exception e) {
            if (mockWhenDisabled) return true; // 开发环境兼容
            throw new ApiException(500, "验签失败: " + e.getMessage());
        }
    }

    private String buildSignatureData(ShareAccessLog log) {
        return String.join("|",
                n(log.getAccessIp()),
                log.getAccessTime() != null ? log.getAccessTime().toString() : new Date().toString(),
                String.valueOf(log.getFileId()),
                n(log.getActionType()),
                n(log.getTokenHash())
        );
    }

    private String n(String v) {
        return v == null ? "" : v;
    }

    @Override
    public List<ShareAccessLog> getAllLogs() {
        try {
            return repository.findAll(Sort.by(Sort.Direction.DESC, "accessTime"));
        } catch (Exception e) {
            throw new ApiException(500, "查询访问日志失败");
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

    private String safe(String input) {
        return input == null ? "" : input;
    }
}
