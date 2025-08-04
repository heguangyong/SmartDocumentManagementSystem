package com.github.sdms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sdms.dto.UserInfo;
import com.github.sdms.exception.ApiException;
import com.github.sdms.util.UserInfoCache;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 此类用于在生产环境直接通过uid调用接口访问用户数据并缓存。
 * 本项目用户表中并未存储用户全部的信息，均是通过这种模式动态获取，并缓存后提供备用。
 * 如有需要，提供缓存策略，允许定期过去
 */
@Service
public class OAuthUserInfoService {

    @Value("${oauth.client_id}")
    private  String clientId;
    @Value("${oauth.client_secret}")
    private  String clientSecret;
    @Value("${oauth.api_url}")
    private  String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuthUserInfoService(){

    }

    public OAuthUserInfoService(String clientId, String clientSecret, String apiUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apiUrl = apiUrl;
    }

    public boolean contains(String uid) {
        return UserInfoCache.get(uid) != null;
    }

    public UserInfo getUserInfoByUid(String uid) {
        UserInfo cached = UserInfoCache.get(uid);
        if (cached != null) return cached;

        UserInfo fetched = fetchUserInfoFromApi(uid);
        if (fetched != null) UserInfoCache.put(fetched);
        return fetched;
    }

    public List<UserInfo> searchUsers(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return UserInfoCache.listAll().stream().collect(Collectors.toList());
        }
        String lowerKey = keyword.toLowerCase();
        return UserInfoCache.listAll().stream()
                .filter(u -> (u.mobile != null && u.mobile.toLowerCase().contains(lowerKey))
                        || (u.nameCn != null && u.nameCn.toLowerCase().contains(lowerKey))
                        || (u.namePinyin != null && u.namePinyin.toLowerCase().contains(lowerKey))
                        || (u.username != null && u.username.toLowerCase().contains(lowerKey)))
                .collect(Collectors.toList());
    }

    private UserInfo fetchUserInfoFromApi(String unionId) {
        try {
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String sign = DigestUtils.sha256Hex(clientId + timestamp + clientSecret);

            String jsonData = "{\"unionId\":\"" + unionId + "\"}";

            String key = clientSecret.substring(0, 32);
            String iv = clientSecret.substring(clientSecret.length() - 16);

            byte[] encrypted = encryptAES(jsonData, key, iv);
            String base64Encoded = Base64.encodeBase64String(encrypted);
            String urlEncodedData = URLEncoder.encode(base64Encoded, StandardCharsets.UTF_8.toString()).replace("+", "%2B");

            String fullUrl = String.format("%s?clientId=%s&timestamp=%s&sign=%s&data=%s",
                    apiUrl, clientId, timestamp, sign, urlEncodedData);

            HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("x-oauth-encoding-data", "encoded");
            conn.setRequestProperty("x-oauth-unionid", unionId);

            if (conn.getResponseCode() != 200) {
                throw new ApiException(conn.getResponseCode(), "请求失败，HTTP状态码：" + conn.getResponseCode());
            }

            String response = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();

            JsonNode rootNode = objectMapper.readTree(response);
            int code = rootNode.path("resultStatus").path("code").asInt();
            if (code != 1) {
                String msg = rootNode.path("resultStatus").path("message").asText();
                throw new ApiException(code, "接口返回错误，code=" + code + ", msg=" + msg);
            }

            String encryptedResult = rootNode.path("resultValue").asText();
            if (encryptedResult == null || encryptedResult.isEmpty()) {
                throw new ApiException(400, "接口返回加密内容为空");
            }

            byte[] decodedResult = Base64.decodeBase64(encryptedResult);
            String decryptedJson = decryptAES(decodedResult, key, iv);

            return objectMapper.readValue(decryptedJson, UserInfo.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("调用接口失败：" + e.getMessage());
        }
    }

    private byte[] encryptAES(String data, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptAES(byte[] encrypted, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }


    public PagedResult<UserInfo> searchUsersPaged(String keyword, int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        List<UserInfo> all = searchUsers(keyword); // 已实现模糊搜索
        int fromIndex = Math.min((page - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());

        List<UserInfo> pageItems = all.stream()
                .sorted(Comparator.comparing(u -> Optional.ofNullable(u.nameCn).orElse("")))
                .collect(Collectors.toList())
                .subList(fromIndex, toIndex);

        return new PagedResult<>(pageItems, all.size());
    }

}
