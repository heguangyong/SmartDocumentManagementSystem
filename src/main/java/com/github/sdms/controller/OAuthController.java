package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.UUserReq;
import com.github.sdms.dto.UserProfileDto;
import com.github.sdms.model.User;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "获取授权跳转地址（download）（所有角色可访问，未登录可跳转授权）")
    @GetMapping("/redirect/download")
    public void redirectToOauth1(@RequestParam String libraryCode, HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.getOauthRedirectUri("1", libraryCode));
    }

    @Operation(summary = "获取授权跳转地址（upload）（所有角色可访问，未登录可跳转授权）")
    @GetMapping("/redirect/upload")
    public void redirectToOauth2(@RequestParam String libraryCode, HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.getOauthRedirectUri("2", libraryCode));
    }

    @Operation(summary = "授权回调处理（download）（所有角色可访问）")
    @GetMapping("/callback")
    public void handleAuthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam String libraryCode,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = authService.handleCallback(code, state, downloadUrl, libraryCode);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "授权回调处理（upload）（所有角色可访问）")
    @GetMapping("/callback-upload")
    public void handleAuthCallbackUpload(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam String libraryCode,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = authService.handleCallback(code, state, uploadUrl, libraryCode);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "根据code获取当前用户信息（READER/LIBRARIAN/ADMIN）")
    @PostMapping("/userinfo")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> getUserInfo(@RequestBody UUserReq req, @RequestParam String libraryCode, HttpServletResponse response) throws IOException {
        String result = authService.getUserInfoByCode(req, libraryCode, response);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "用户登出（READER/LIBRARIAN/ADMIN）")
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> logout(@RequestParam("uid") String uid) {
        return ResponseEntity.ok(ApiResponse.success(authService.logout(uid)));
    }

    @Operation(summary = "会话检查（READER/LIBRARIAN/ADMIN）")
    @PostMapping("/session/check")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkSession(
            @RequestParam("uid") String uid,
            @RequestParam("libraryCode") String libraryCode, // 新增 libraryCode 参数
            HttpServletRequest request
    ) {
        // 调用 service 层的 checkSession 方法，传递 libraryCode 参数
        return ResponseEntity.ok(ApiResponse.success(authService.checkSession(uid, request.getRequestURI(), libraryCode)));
    }


    @Operation(summary = "根据 accessToken 查询用户信息（仅 ADMIN）")
    @GetMapping("/userinfo/accessToken")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> getUserInfoByAccessToken(@RequestParam("accessToken") String accessToken, @RequestParam String libraryCode) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(authService.getUserInfoByAccessToken(accessToken, libraryCode)));
    }

    @Operation(summary = "登录成功会话处理（READER/LIBRARIAN/ADMIN）")
    @PostMapping("/session/set")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> loginset(@RequestParam("uid") String uid, @RequestParam String libraryCode) {
        return ResponseEntity.ok(ApiResponse.success(authService.setLogin(uid,libraryCode)));
    }

    @Operation(summary = "获取当前登录用户信息（READER/LIBRARIAN/ADMIN）")
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(Authentication authentication, @RequestParam String libraryCode) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        String username = authentication.getName();
        // 使用 libraryCode 查询username和 UID 进行匹配
        User user = userRepository.findByUsernameAndLibraryCode(username, libraryCode)
                .orElseGet(() -> userRepository.findByUidAndLibraryCode(username, libraryCode).orElse(null));

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("User not found"));
        }

        UserProfileDto dto = UserProfileDto.builder()
                .uid(user.getUid())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRoleType().toString())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }


}
