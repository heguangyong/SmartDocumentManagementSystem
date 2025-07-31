package com.github.sdms.service.impl;

import com.github.sdms.exception.ApiException;
import com.github.sdms.model.ShareAccessLog;
import com.github.sdms.repository.ShareAccessLogRepository;
import com.github.sdms.service.ShareAccessLogService;
import com.koalii.svs.client.Svs2ClientHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareAccessLogServiceImpl implements ShareAccessLogService {

    private final ShareAccessLogRepository repository;
    private final Svs2ClientHelper helper;

    @Value("${svs.service.enabled:true}")
    private boolean signatureEnabled;

    @Override
    @Transactional
    public void recordAccess(ShareAccessLog log) {
        try {
            if (signatureEnabled) {
                String data = buildSignatureData(log);
                String b64Data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));

                Svs2ClientHelper.SvsResultData result = helper.cdbPkcs7DetachSignEx(b64Data.getBytes(StandardCharsets.UTF_8), "sm2");

                if (result.m_errno != 0 || result.m_b64SignedData == null) {
                    throw new ApiException(500, "签名失败：错误码 " + result.m_errno);
                }

                log.setSignature(result.m_b64SignedData);
            } else {
                log.setSignature("mock-signature-disabled");
            }
            repository.save(log);
        } catch (Exception e) {
            throw new ApiException(500, "记录访问日志失败: " + e.getMessage());
        }
    }

    @Override
    public List<ShareAccessLog> getAllLogs() {
        try {
            return repository.findAll(Sort.by(Sort.Direction.DESC, "accessTime"));
        } catch (Exception e) {
            throw new ApiException(500, "查询访问日志失败");
        }
    }

    public boolean verifyAccessLogSignature(ShareAccessLog log) {
        if (!signatureEnabled) {
            return true; // 跳过验签
        }
        try {
            String data = buildSignatureData(log);
            String b64Data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));

            Svs2ClientHelper.SvsResultData verifyResult = helper.cdbPkcs7DetachVerifyEx(log.getSignature(), b64Data.getBytes(StandardCharsets.UTF_8));

            return verifyResult.m_errno == 0;
        } catch (Exception e) {
            throw new ApiException(500, "验签失败: " + e.getMessage());
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
