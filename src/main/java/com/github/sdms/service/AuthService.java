package com.github.sdms.service;

import com.github.sdms.dto.UUserReq;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthService {

    /**
     * 获取第三方OAuth授权跳转地址
     * @param type OAuth平台类型（如钉钉/飞书）
     * @param libraryCode 租户标识
     * @return 跳转地址
     */
    String getOauthRedirectUri(String type, String libraryCode);

    /**
     * OAuth登录回调处理
     * @param code 授权码
     * @param state 状态标识（如区分v3/v5）
     * @param baseRedirectUrl 回跳前端地址
     * @param libraryCode 租户标识
     * @return 拼接了token的回跳地址
     */
    String handleCallback(String code, String state, String baseRedirectUrl, String libraryCode);

    /**
     * 根据自定义Token获取用户信息
     * @param req 包含code和token参数的请求体
     * @param libraryCode 租户标识
     * @param response HttpServletResponse对象（用于输出错误信息）
     * @return 用户信息（uid + username）
     */
    String getUserInfoByCode(UUserReq req, String libraryCode, HttpServletResponse response) throws IOException;

    /**
     * 登出用户，删除Redis中的登录态
     * @param uid 用户唯一标识
     * @return 登出结果提示
     */
    String logout(String uid);

    /**
     * 检查当前用户Session是否过期
     * @param uid 用户ID
     * @param path 请求路径（用于白名单判断）
     * @param libraryCode 租户代码，用于区分不同租户的用户会话
     * @return "session valid" 或 "session timeout"
     */
    String checkSession(String uid, String path, String libraryCode);


    /**
     * 设置当前用户的登录时间，用于后续登录态检测
     * @param uid 用户ID
     * @param libraryCode 租户代码，用于区分不同租户的用户会话
     * @return 操作结果
     */
    String setLogin(String uid, String libraryCode);

    /**
     * 根据OAuth Token获取用户信息，并写入本地用户表
     * @param accessToken 第三方平台accessToken
     * @param libraryCode 租户标识
     * @return 用户信息字符串（uid===username===token）
     * @throws Exception 异常处理
     */
    String getUserInfoByAccessToken(String accessToken, String libraryCode) throws Exception;
}
