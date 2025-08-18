package com.github.sdms.service;

import com.github.sdms.config.KmsProperties;
import com.koal.kms.sdk.ed.KmsSdkException;
import com.koal.kms.sdk.ed.KmsUtil;
import com.koal.kms.sdk.ed.Mode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class KmsCryptoService {

    private final Mode mode;
    private final byte[] iv;

    public KmsCryptoService(KmsProperties props) {
        this.mode = Mode.valueOf(props.getMode());
        this.iv = Base64.getDecoder().decode(props.getIv());
        // KmsUtil config is read from kms_client.yml on classpath as your project already does
    }

    public String encryptToB64(String plain) {
        if (plain == null) return null;
        try {
            byte[] cipher = KmsUtil.encrypt(plain.getBytes(StandardCharsets.UTF_8), mode, iv);
            return Base64.getEncoder().encodeToString(cipher);
        } catch (KmsSdkException e) {
            log.error("KMS encrypt failed", e);
            throw new RuntimeException(e);
        }
    }

    public String decryptFromB64(String b64) {
        if (b64 == null) return null;
        try {
            byte[] cipher = Base64.getDecoder().decode(b64);
            byte[] plain = KmsUtil.decrypt(cipher, mode, iv);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (KmsSdkException e) {
            log.error("KMS decrypt failed", e);
            throw new RuntimeException(e);
        }
    }
}