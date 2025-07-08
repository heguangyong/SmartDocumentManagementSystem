package com.Ayush.sdms_backend.auth.controller;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSONObject;
import com.Ayush.sdms_backend.auth.service.AuthService;
import com.Ayush.sdms_backend.auth.dto.UUserReq;
import com.Ayush.sdms_backend.common.util.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class OAuthController {

    @Value("${myset.download_url_1}")
    private String downloadUrl;

    @Value("${myset.upload_url_2}")
    private String uploadUrl;

    @Resource
    private AuthService authService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "获取授权跳转地址（download）")
    @GetMapping("/redirect/download")
    public void redirectToOauth1(HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.getOauthRedirectUri("1"));
    }

    @Operation(summary = "获取授权跳转地址（upload）")
    @GetMapping("/redirect/upload")
    public void redirectToOauth2(HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.getOauthRedirectUri("2"));
    }

    @Operation(summary = "授权回调处理（download）")
    @GetMapping("/callback")
    public void handleAuthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = authService.handleCallback(code, state, downloadUrl);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "授权回调处理（upload）")
    @GetMapping("/callback-upload")
    public void handleAuthCallbackUpload(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = authService.handleCallback(code, state, uploadUrl);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "根据code获取当前用户信息")
    @PostMapping("/userinfo")
    public String getUserInfo(@RequestBody UUserReq req, HttpServletResponse response) throws IOException {
        return authService.getUserInfoByCode(req, response);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public String logout(@RequestParam("uid") String uid) {
        return authService.logout(uid);
    }

    @Operation(summary = "会话检查")
    @PostMapping("/session/check")
    public String checkSession(@RequestParam("uid") String uid, HttpServletRequest request) {
        return authService.checkSession(uid, request.getRequestURI());
    }

    @Operation(summary = "根据 accessToken 查询用户信息")
    @GetMapping("/userinfo/accessToken")
    public String getUserInfoByAccessToken(@RequestParam("accessToken") String accessToken) throws Exception {
        return authService.getUserInfoByAccessToken(accessToken);
    }

    @Operation(summary = "登录成功会话处理")
    @PostMapping("/session/set")
    public String loginset(@RequestParam("uid") String uid) {
        return authService.setLogin(uid);
    }

}
