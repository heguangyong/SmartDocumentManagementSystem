package com.github.sdms.util;

import com.github.sdms.model.User;
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
    @Value("${jwt.expiration:7200000}") // 2小时默认
    private long EXPIRATION_TIME;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    private static final List<String> ROLE_PRIORITY = List.of("admin", "librarian", "reader");

    // ========== JWT 生成相关方法 ==========

    /**
     * 生成 JWT - 本地登录用，支持多角色
     */
    public String generateToken(UserDetails userDetails, String libraryCode) {
        Long userId = ((CustomerUserDetails) userDetails).getUserId();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", userDetails.getUsername());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        claims.put("libraryCode", libraryCode);
        claims.put("iss", determineIssuer(roles));

        return buildToken(claims, String.valueOf(userId));
    }

    /**
     * 生成 JWT - OAuth 登录用，支持单角色（可扩展为多角色）
     */
    public String generateToken(Long userId, List<String> roles, String libraryCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("libraryCode", libraryCode);
        claims.put("iss", determineIssuer(roles));

        return buildToken(claims, String.valueOf(userId));
    }

    public String generateToken(UserDetails userDetails, String libraryCode, boolean rememberMe) {
        Long userId = ((CustomerUserDetails) userDetails).getUserId();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toList());
        return generateToken(userId, roles, libraryCode, rememberMe);
    }

    public String generateToken(Long userId, List<String> roles, String libraryCode, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("libraryCode", libraryCode);
        claims.put("iss", determineIssuer(roles));
        long expiration = rememberMe ? 90L * 24 * 60 * 60 * 1000 : EXPIRATION_TIME;
        return buildToken(claims, String.valueOf(userId), expiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return buildToken(claims, subject, EXPIRATION_TIME);
    }

    // ========== JWT 解析相关方法 ==========

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

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            return null;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration == null || expiration.before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Long tokenUserId = extractUserId(token);
        if (userDetails instanceof CustomerUserDetails) {
            CustomerUserDetails customerDetails = (CustomerUserDetails) userDetails;
            return tokenUserId != null &&
                    tokenUserId.equals(customerDetails.getUserId()) &&
                    !isTokenExpired(token);
        }
        return false;
    }

    public Long extractUserId(String token) {
        Long userId = extractClaim(token, claims -> claims.get("userId", Long.class));
        if (userId == null) {
            throw new IllegalArgumentException("User ID not found in token");
        }
        return userId;
    }

    private String extractLibraryCode(String token) {
        return extractAllClaims(token).get("libraryCode", String.class);
    }

    private String determineIssuer(List<String> roles) {
        return roles.stream()
                .map(String::toLowerCase)
                .filter(ROLE_PRIORITY::contains)
                .min(Comparator.comparingInt(ROLE_PRIORITY::indexOf))
                .orElse("reader");
    }

    // ========== 当前用户信息获取方法（合并AuthUtils功能）==========

    /**
     * 获取当前认证对象
     */
    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    /**
     * 获取当前登录用户对象（核心方法）
     * @return 当前用户对象，如果未登录返回null
     */
    public static User getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomerUserDetails userDetails) {
            return userDetails.getUser();
        }

        return null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * 获取当前登录用户ID（带异常）- 替代JwtUtil.getCurrentUserIdOrThrow()
     */
    public static Long getCurrentUserIdOrThrow() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未认证");
        }
        return userId;
    }

    /**
     * 获取当前用户的UID - 替代JwtUtil.getCurrentUidOrThrow()
     */
    public static String getCurrentUid() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    /**
     * 获取当前用户的UID（带异常）- 替代JwtUtil.getCurrentUidOrThrow()
     */
    public static String getCurrentUidOrThrow() {
        String uid = getCurrentUid();
        if (uid == null) {
            throw new RuntimeException("用户未认证");
        }
        return uid;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUsername() : null;
    }

    /**
     * 获取当前用户的libraryCode
     */
    public static String getCurrentLibraryCode() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getLibraryCode() : null;
    }

    /**
     * 获取当前用户角色（去除ROLE_前缀）
     */
    public static String getCurrentRole() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(role -> role.startsWith("ROLE_"))
                    .map(role -> role.substring(5))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        String role = getCurrentRole();
        return "ADMIN".equals(role);
    }

    /**
     * 判断当前用户是否为图书管理员
     */
    public static boolean isLibrarian() {
        String role = getCurrentRole();
        return "LIBRARIAN".equals(role);
    }

    /**
     * 判断当前用户是否为读者
     */
    public static boolean isReader() {
        String role = getCurrentRole();
        return "READER".equals(role);
    }
}