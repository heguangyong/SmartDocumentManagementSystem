package com.github.sdms.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

public class JwtKeyGenerator {
    public static void main(String[] args) {
        // 方案1：扩展现有密钥（推荐）
        String currentKey = "63f4945d921d599f27ae4fdf5bada3f1a1b2c3d4e5f678901234567890abcdef";
        String extendedKey = extendKeyToSecureLength(currentKey);
        System.out.println("=== 扩展现有密钥（推荐，兼容现有token） ===");
        System.out.println("jwt.secret: " + extendedKey);
        System.out.println("长度: " + extendedKey.getBytes().length + " bytes");

        System.out.println("\n=== 生成全新安全密钥（需要重新登录） ===");
        // 方案2：生成全新密钥
        SecretKey newKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String encodedNewKey = Base64.getEncoder().encodeToString(newKey.getEncoded());
        System.out.println("jwt.secret: " + encodedNewKey);
        System.out.println("长度: " + newKey.getEncoded().length + " bytes");
    }

    private static String extendKeyToSecureLength(String originalKey) {
        StringBuilder sb = new StringBuilder(originalKey);
        while (sb.length() < 64) {
            sb.append(originalKey);
        }
        return sb.substring(0, 64);
    }
}
