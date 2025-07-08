package com.Ayush.sdms_backend.common.util;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.Map;

public class ServletUtils {

    /**
     * 获取当前请求对象
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 获取 Origin 头
     */
    public static String getOrigin() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("Origin") : null;
    }

    /**
     * 获取 Referer 头
     */
    public static String getRefer() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("Referer") : null;
    }

    /**
     * 获取 Referer 多值头
     */
    public static Enumeration<String> getRefer2() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeaders("Origin") : null;
    }

    /**
     * 获取请求参数 Map
     */
    public static Map<String, String[]> getParameterMap() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getParameterMap() : null;
    }

    /**
     * 获取客户端真实 IP 地址（支持代理）
     */
    public static String getClientIP() {
        HttpServletRequest request = getRequest();
        return request != null ? JakartaServletUtil.getClientIP(request) : null;
    }

    /**
     * 获取远程主机地址
     */
    public static String getHost() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRemoteHost() : null;
    }

    /**
     * 获取请求 URI（不包含域名和端口）
     */
    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 获取请求完整 URL
     */
    public static String getRequestURL() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURL().toString() : null;
    }

    /**
     * 获取 X-Real-IP 头
     */
    public static String getRealIp() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("X-Real-IP") : null;
    }

    /**
     * 获取 X-Forwarded-For 头
     */
    public static String getXForwardedFor() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("X-Forwarded-For") : null;
    }
}
