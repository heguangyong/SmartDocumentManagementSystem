package com.github.sdms.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.Instant;

public class ApiRequestHelper {

    static {
        Security.addProvider(new BouncyCastleProvider()); // 添加 BouncyCastle 提供者，支持 PKCS7Padding
    }

    public static void main(String[] args) throws Exception {
        String clientId = "1000855101";
        String clientSecret = "bf5a28d7ff094b6ec91cea1b655bf1863e02c2daa140598fcd198bcb0f0349fb";
        String unionId = "o0Qj7bzn6WL05gmWEgB2OlXd";

        // 1. 当前时间戳（毫秒）
        String timestamp = String.valueOf(Instant.now().toEpochMilli());

        // 2. 签名生成（SHA256）
        String signInput = clientId + timestamp + clientSecret;
        String sign = DigestUtils.sha256Hex(signInput);

        // 3. 构造 JSON 请求体
        String jsonData = "{\"unionId\":\"" + unionId + "\"}";

        // 4. 加密参数（AES-256-CBC + PKCS7Padding）
        String key = clientSecret.substring(0, 32); // AES-256 key
        String iv = clientSecret.substring(clientSecret.length() - 16); // IV

        byte[] encryptedBytes = encryptWithAES(jsonData, key, iv);
        String base64Encrypted = Base64.encodeBase64String(encryptedBytes);
        String encodedData = URLEncoder.encode(base64Encrypted, StandardCharsets.UTF_8.toString())
                .replace("+", "%2B"); // 防止 "+" 被当空格解析

        // 5. 构造完整请求 URL
        String url = String.format(
                "https://passport.library.sh.cn:4430/rs/api/v4/infoByUnionId?clientId=%s&timestamp=%s&sign=%s&data=%s",
                clientId, timestamp, sign, encodedData
        );

        // 输出调试信息
        System.out.println("请求 URL：\n" + url);
        System.out.println("请求头：");
        System.out.println("x-oauth-encoding-data: encoded");
        System.out.println("x-oauth-unionid: " + unionId);
    }

    private static byte[] encryptWithAES(String data, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC"); // 使用 BouncyCastle 的 PKCS7Padding
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
