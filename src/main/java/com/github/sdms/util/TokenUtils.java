package com.github.sdms.util;

import org.apache.commons.codec.digest.DigestUtils;

public class TokenUtils {
    public static String hashToken(String token) {
        return DigestUtils.sha256Hex(token); // Apache Commons Codec
    }
}
