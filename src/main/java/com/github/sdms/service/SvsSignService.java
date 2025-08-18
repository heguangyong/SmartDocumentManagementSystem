package com.github.sdms.service;

import com.github.sdms.config.SvsClientProperties;
import com.koalii.svs.client.Svs2ClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SvsSignService {

    private final Optional<Svs2ClientHelper> helperOpt;
    private final SvsClientProperties props;

    public String signB64(String originData) {
        if (!props.isEnabled() || helperOpt.isEmpty()) {
            // mock or raise depending on config; we follow sdms.crypto.mock-when-disabled externally
            throw new IllegalStateException("SVS not enabled or helper not present");
        }
        Svs2ClientHelper helper = helperOpt.get();
        Svs2ClientHelper.SvsResultData r = helper.cdbSignData(originData.getBytes(StandardCharsets.UTF_8), props.getCertId());
        if (r.m_errno != 0 || r.m_b64SignedData == null) {
            throw new IllegalStateException("SVS sign failed, errno=" + r.m_errno + " msg=" + r.m_message);
        }
        return r.m_b64SignedData;
    }

    public boolean verify(String originData, String b64Signed) {
        if (!props.isEnabled() || helperOpt.isEmpty()) return true;
        Svs2ClientHelper helper = helperOpt.get();
        Svs2ClientHelper.SvsResultData r = helper.cdbVerifySign(originData.getBytes(StandardCharsets.UTF_8), b64Signed, props.getCertId());
        return r.m_errno == 0;
    }
}