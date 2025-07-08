package com.Ayush.sdms_backend.auth.service.impl;

import cn.hutool.core.util.URLUtil;
import com.Ayush.sdms_backend.model.AppUser;
import com.Ayush.sdms_backend.repository.UserRepository;
import com.Ayush.sdms_backend.storage.minio.MinioClientService;
import com.alibaba.fastjson2.JSONObject;
import com.Ayush.sdms_backend.auth.dto.UUserReq;
import com.Ayush.sdms_backend.auth.service.AuthService;
import com.Ayush.sdms_backend.common.util.ServletUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${myset.download_url_1}")
    private String downloadUrl;

    @Value("${myset.upload_url_2}")
    private String uploadUrl;

    @Resource
    private HttpClient httpClient;

    @Resource
    private UserRepository userRepository;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MinioClientService minioClientService;

    @Override
    public String getOauthRedirectUri(String type) {
        return httpClient.oauthRedirectUri(type);
    }

    @Override
    public String handleCallback(String code, String state, String baseRedirectUrl) {
        String accessToken = httpClient.getOauthToken(code);
        JSONObject userInfo = httpClient.userinfoByAccessToken(accessToken, state.equals("2") ? "v5" : "v3");
        String uid = userInfo.getString("x-oauth-unionid");
        String username = userInfo.getString("nameCn");

        AppUser user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setUserinfo(null);
            user.setIp(ServletUtils.getClientIP());
            userRepository.save(user);  // JPA 自动 insert
        }

        String token = generateToken(uid);
        stringRedisTemplate.opsForValue().set(token + "_auth", uid + "===" + URLUtil.encode(username));
        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

        return baseRedirectUrl + "?code=" + token;
    }

    @Override
    public String getUserInfoByCode(UUserReq req, HttpServletResponse response) throws IOException {
        Map<String, Object> params = req.getMap();
        String urlCheck = minioClientService.urltoken(params);
        if (!"ok!".equals(urlCheck)) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("URL token Failed verification!");
            return "";
        }

        String redisKey = req.getCode() + "_auth";
        String userInfo = stringRedisTemplate.opsForValue().get(redisKey);
        if (userInfo != null) {
            stringRedisTemplate.delete(redisKey);
        }
        return userInfo != null ? userInfo : "";
    }

    @Override
    public String logout(String uid) {
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken_" + uid);
        if (accessToken != null) {
            stringRedisTemplate.delete("accessToken_" + uid);
            httpClient.userLogout(accessToken);
        }
        return "logout success";
    }

    @Override
    public String checkSession(String uid, String path) {
        return "timeout".equals(minioClientService.logintimecheck(uid, path)) ? "session timeout" : "session valid";
    }

    @Override
    public String setLogin(String uid) {
        minioClientService.loginset(uid);
        return "loginset success";
    }

    @Override
    public String getUserInfoByAccessToken(String accessToken) throws Exception {
        JSONObject userInfo = httpClient.userinfoByAccessToken(accessToken, "v5");
        String uid = userInfo.getString("x-oauth-unionid");
        String username = userInfo.getString("nameCn");

        AppUser user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setUserinfo(null);
            user.setIp(ServletUtils.getClientIP());
            userRepository.save(user);  // JPA 自动 insert
        }


        String token = generateToken(uid);
        stringRedisTemplate.opsForValue().set(token + "_auth", uid + "===" + URLUtil.encode(username));
        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);
        return uid + "===" + URLUtil.encode(username) + "===" + token;
    }

    private String generateToken(String uid) {
        long timestamp = System.currentTimeMillis();
        String content = timestamp + "-" + uid;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder buf = new StringBuilder();
            for (byte b : hashBytes) {
                int i = b;
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
