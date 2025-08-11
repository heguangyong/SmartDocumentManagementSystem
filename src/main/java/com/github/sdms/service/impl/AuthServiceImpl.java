package com.github.sdms.service.impl;

import cn.hutool.core.util.URLUtil;
import com.github.sdms.dto.UUserReq;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.AuthService;
import com.github.sdms.service.MinioService;
import com.github.sdms.util.JwtUtil;
import com.github.sdms.util.OAuthClient;
import com.github.sdms.util.ServletUtils;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
    private MinioService minioService;

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    public String getOauthRedirectUri(String type, String libraryCode) {
        return oAuthClient.oauthRedirectUri(type);
    }

    @Override
    public String handleCallback(String code, String state, String baseRedirectUrl, String libraryCode) {
        String accessToken = oAuthClient.getOauthToken(code);
        if (accessToken == null || accessToken.isEmpty()) {
            throw new ApiException(400, "无法获取访问令牌");
        }

        JSONObject userInfo = oAuthClient.userinfoByAccessToken(accessToken, state.equals("2") ? "v5" : "v3");
        if (userInfo == null || userInfo.isEmpty()) {
            throw new ApiException(400, "无法获取用户信息");
        }

        String uid = userInfo.getString("x-oauth-unionid");
        if (uid == null || uid.isEmpty()) {
            throw new ApiException(400, "用户唯一标识不存在");
        }

        String username = userInfo.getString("nameCn");

        List<String> rolesFromFolio = new ArrayList<>();
        if (userInfo.containsKey("roles")) {
            var roles = userInfo.getJSONArray("roles");
            for (Object r : roles) {
                String role = r.toString().toUpperCase();
                if (role.equals("ADMIN") || role.equals("LIBRARIAN") || role.equals("READER")) {
                    rolesFromFolio.add(role);
                }
            }
        }
        if (rolesFromFolio.isEmpty()) {
            rolesFromFolio.add("READER");
        }

        // 注册或更新用户，获取 userId
        User user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
        if (user == null) {
            user = User.builder()
                    .uid(uid)
                    .username(username != null ? username : "")
                    .libraryCode(libraryCode)
                    .roleType(RoleType.valueOf(rolesFromFolio.get(0)))
                    .build();
        } else {
            user.setRoleType(RoleType.valueOf(rolesFromFolio.get(0)));
            user.setUsername(username);
        }
        user = userRepository.save(user);
        Long userId = user.getId();

        // 生成 JWT 用于前端认证
        String jwt = jwtUtil.generateToken(userId, rolesFromFolio, libraryCode);
        stringRedisTemplate.opsForValue().set("accessToken_" + userId, accessToken);

        return baseRedirectUrl + "?code=" + jwt;
    }

    @Override
    public String getUserInfoByCode(UUserReq req, String libraryCode, HttpServletResponse response) throws IOException {
        Map<String, Object> params = req.getMap();
        String urlCheck = minioService.urltoken(params);
        if (!"ok!".equals(urlCheck)) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("URL token Failed verification!");
            throw new ApiException(403, "URL token 验证失败");
        }

        String jwt = req.getCode();
        String uid = jwtUtil.extractUsername(jwt);
        if (uid == null) {
            throw new ApiException(401, "无效的Token");
        }

        User user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
        String username = user != null ? user.getUsername() : "";

        return uid + "===" + username + "===" + jwt;
    }


    @Override
    public String logout(String uid) {
        String key = "accessToken_" + uid;
        String accessToken = stringRedisTemplate.opsForValue().get(key);
        if (accessToken != null) {
            stringRedisTemplate.delete(key);
            oAuthClient.userLogout(accessToken);
        }
        return "logout success";
    }

    @Override
    public String checkSession(String uid, String path, String libraryCode) {
        String status = minioService.logintimecheck(uid, path, libraryCode);
        if ("timeout".equals(status)) {
            throw new ApiException(401, "会话已过期，请重新登录");
        }
        return "session valid";
    }

    @Override
    public String setLogin(String uid, String libraryCode) {
        minioService.loginset(uid, libraryCode);
        return "loginset success";
    }

    @Override
    public String getUserInfoByAccessToken(String accessToken, String libraryCode) throws Exception {
        JSONObject userInfo = oAuthClient.userinfoByAccessToken(accessToken, "v5");
        if (userInfo == null || userInfo.isEmpty()) {
            throw new ApiException(400, "无法获取用户信息");
        }

        String uid = userInfo.getString("x-oauth-unionid");
        if (uid == null || uid.isEmpty()) {
            throw new ApiException(400, "用户唯一标识不存在");
        }

        String username = userInfo.getString("nameCn");

        User user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
        if (user == null) {
            user = User.builder()
                    .uid(uid)
                    .username(username != null ? username : "")
                    .libraryCode(libraryCode)
                    .ip(ServletUtils.getClientIP())
                    .build();
            user = userRepository.save(user);
        }

        org.springframework.security.core.userdetails.User jwtUser =
                new org.springframework.security.core.userdetails.User(uid, "", java.util.Collections.emptyList());

        String jwt = jwtUtil.generateToken(jwtUser, libraryCode);
        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

        return uid + "===" + URLUtil.encode(username) + "===" + jwt;
    }

}
