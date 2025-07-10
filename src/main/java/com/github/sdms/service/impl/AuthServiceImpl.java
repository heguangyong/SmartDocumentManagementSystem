package com.github.sdms.service.impl;

import cn.hutool.core.util.URLUtil;
import com.github.sdms.components.JwtUtil;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.MinioClientService;
import com.github.sdms.thirdparty.oauth.OAuthClient;
import com.alibaba.fastjson2.JSONObject;
import com.github.sdms.dto.UUserReq;
import com.github.sdms.service.AuthService;
import com.github.sdms.common.util.ServletUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${myset.download_url_1}")
    private String downloadUrl;

    @Value("${myset.upload_url_2}")
    private String uploadUrl;

    @Resource
    private OAuthClient oAuthClient;

    @Resource
    private UserRepository userRepository;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MinioClientService minioClientService;

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    public String getOauthRedirectUri(String type) {
        return oAuthClient.oauthRedirectUri(type);
    }

    @Override
    public String handleCallback(String code, String state, String baseRedirectUrl) {
        String accessToken = oAuthClient.getOauthToken(code);
        JSONObject userInfo = oAuthClient.userinfoByAccessToken(accessToken, state.equals("2") ? "v5" : "v3");

        String uid = userInfo.getString("x-oauth-unionid");
        String username = userInfo.getString("nameCn");
        String roleFromFolio = "READER"; // 默认角色

        // 获取角色字段（你需确认返回字段名，假设为 roles）
        if (userInfo.containsKey("roles")) {
            var roles = userInfo.getJSONArray("roles");
            if (roles.contains("admin")) roleFromFolio = "ADMIN";
            else if (roles.contains("librarian")) roleFromFolio = "LIBRARIAN";
            else roleFromFolio = "READER";
        }

        AppUser user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setUserinfo(null);
            user.setIp(ServletUtils.getClientIP());
            user.setRole(Role.valueOf(roleFromFolio)); // ✅ 首次设置角色
        } else {
            user.setRole(Role.valueOf(roleFromFolio)); // ✅ 每次同步角色，保持一致
        }
        userRepository.save(user);

        // ✅ 使用 JwtUtil 生成 JWT，带角色
        String jwt = jwtUtil.generateToken(uid, roleFromFolio);

        // 可选：缓存 accessToken
        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

        return baseRedirectUrl + "?code=" + jwt;
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

        // ✅ code 本身是 JWT
        String jwt = req.getCode();
        String uid = jwtUtil.extractUsername(jwt); // subject 就是 uid
        String role = jwtUtil.extractRole(jwt);

        if (uid == null) {
            return "Invalid token";
        }

        // 可选：从数据库获取用户信息做返回
        AppUser user = userRepository.findByUid(uid).orElse(null);
        String username = user != null ? user.getUsername() : "";

        return uid + "===" + username + "===" + jwt;
    }


    @Override
    public String logout(String uid) {
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken_" + uid);
        if (accessToken != null) {
            stringRedisTemplate.delete("accessToken_" + uid);
            oAuthClient.userLogout(accessToken);
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
        JSONObject userInfo = oAuthClient.userinfoByAccessToken(accessToken, "v5");
        String uid = userInfo.getString("x-oauth-unionid");
        String username = userInfo.getString("nameCn");

        AppUser user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setUserinfo(null);
            user.setIp(ServletUtils.getClientIP());
            userRepository.save(user);
        }

        // ✅ 使用统一 JWT 生成逻辑
        org.springframework.security.core.userdetails.User jwtUser =
                new org.springframework.security.core.userdetails.User(uid, "", java.util.Collections.emptyList());
        String jwt = jwtUtil.generateToken(jwtUser);

        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

        return uid + "===" + URLUtil.encode(username) + "===" + jwt;
    }

}
