package com.github.sdms.service.impl;

import cn.hutool.core.util.URLUtil;
import com.github.sdms.dto.UUserReq;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.AppUser;
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

        // 解析角色列表
        List<String> rolesFromFolio = new ArrayList<>();
        if (userInfo.containsKey("roles")) {
            var roles = userInfo.getJSONArray("roles");
            for (Object r : roles) {
                String role = r.toString().toUpperCase();
                // 过滤有效角色，默认reader
                if (role.equals("ADMIN") || role.equals("LIBRARIAN") || role.equals("READER")) {
                    rolesFromFolio.add(role);
                }
            }
        }
        if (rolesFromFolio.isEmpty()) {
            rolesFromFolio.add("READER");
        }

        // 修改为根据 libraryCode 查询
        AppUser user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setRoleType(RoleType.valueOf(rolesFromFolio.get(0))); // 只保留主角色
        } else {
            user.setRoleType(RoleType.valueOf(rolesFromFolio.get(0)));
        }
        userRepository.save(user);

        // 生成 JWT，传递角色列表
        String jwt = jwtUtil.generateToken(uid, rolesFromFolio, libraryCode);

        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

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

        // code 本身是 JWT
        String jwt = req.getCode();
        String uid = jwtUtil.extractUsername(jwt); // subject 就是 uid
        if (uid == null) {
            throw new ApiException(401, "无效的Token");
        }

        String role = jwtUtil.extractRole(jwt);

        // 根据 libraryCode 查询用户
        AppUser user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
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
    public String checkSession(String uid, String path, String libraryCode) {
        // 使用 libraryCode 来进行会话验证，确保每个租户的用户会话是独立的
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

        // 根据 libraryCode 查询用户
        AppUser user = userRepository.findByUidAndLibraryCode(uid, libraryCode).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setUid(uid);
            user.setUsername(username != null ? username : "");
            user.setUserinfo(null);
            user.setIp(ServletUtils.getClientIP());
            userRepository.save(user);
        }

        // 使用统一 JWT 生成逻辑
        org.springframework.security.core.userdetails.User jwtUser =
                new org.springframework.security.core.userdetails.User(uid, "", java.util.Collections.emptyList());
        String jwt = jwtUtil.generateToken(jwtUser, libraryCode);

        stringRedisTemplate.opsForValue().set("accessToken_" + uid, accessToken);

        return uid + "===" + URLUtil.encode(username) + "===" + jwt;
    }

}
