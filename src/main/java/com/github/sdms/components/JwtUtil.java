package com.github.sdms.components;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 生成 JWT - 本地登录用，支持多角色
     */
    public String generateToken(UserDetails userDetails) {
        String subject = userDetails.getUsername();
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", subject);
        // 收集所有角色为列表写入claims
        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        claims.put("roles", roles);

        return buildToken(claims, subject);
    }

    /**
     * 生成 JWT - OAuth 登录用，支持单角色（可扩展为多角色）
     */
    public String generateToken(String uid, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", uid);
        claims.put("roles", roles);

        return buildToken(claims, uid);
    }

    /**
     * 核心构建逻辑
     */
    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2)) // 2 小时
                .signWith(getSigningKey())
                .compact();
    }

    // 解析 roles 列表
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(obj -> obj instanceof String)
                    .map(obj -> (String) obj)
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * 提取用户名（subject）
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 提取角色
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * 通用提取 claim 方法
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解码所有 claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查 token 是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration == null || expiration.before(new Date());
    }

    /**
     * 校验 token 是否有效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null &&
                username.equals(userDetails.getUsername()) &&
                !isTokenExpired(token);
    }

    // ✅ 获取当前登录用户的角色（去除 "ROLE_" 前缀）
    public String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(role -> role.startsWith("ROLE_"))
                    .map(role -> role.substring(5)) // 去掉 "ROLE_"
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // ✅ 判断当前用户是否为管理员
    public boolean isAdmin() {
        String role = getCurrentRole();
        return "ADMIN".equals(role);
    }

    // ✅ 获取当前登录用户的 UID（即 JWT 的 subject）
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

}
