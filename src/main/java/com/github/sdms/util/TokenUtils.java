package com.github.sdms.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;

public class TokenUtils {
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    public static String hashToken(String token) {
        return DigestUtils.sha256Hex(token); // Apache Commons Codec
    }
}
