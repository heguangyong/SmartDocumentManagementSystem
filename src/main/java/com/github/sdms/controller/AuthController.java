package com.github.sdms.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.github.sdms.dto.ApiResponse;
import com.github.sdms.dto.LoginRequest;
import com.github.sdms.dto.LoginResponse;
import com.github.sdms.dto.RegisterRequest;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.CustomUserDetailsServices;
import com.github.sdms.util.JwtUtil;
import com.github.sdms.util.PasswordUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth/local")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsServices customUserDetailsServices;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate; // ✅ 注入 RedisTemplate

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_SECONDS = 30 * 60; // 30分钟

    /**
     * 任何已登录用户都可以访问此接口，用于测试权限
     * 允许所有角色访问：READER、LIBRARIAN、ADMIN
     */
    @Operation(summary = "测试权限接口")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    /**
     * 管理员注册馆员
     * 一般允许匿名访问或仅ADMIN访问（这里开放匿名，若需可加 @PreAuthorize("hasRole('ADMIN')")）
     */
    @Operation(summary = "管理员注册馆员")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        try {
            // 检查邮箱和租户组合唯一性
            if (userRepository.existsByEmailAndLibraryCode(request.getEmail(), request.getLibraryCode())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Email already exists for this libraryCode"));
            }

            // 校验角色合法性
            RoleType roleType;
            try {
                roleType = RoleType.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid role: " + request.getRole()));
            }

            // 校验租户代码非空
            String libraryCode = request.getLibraryCode();
            if (libraryCode == null || libraryCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("libraryCode must not be empty"));
            }

            // 新增密码强度校验
            if (!PasswordUtil.isStrongPassword(request.getPassword())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Password must be at least 8 characters and include uppercase, lowercase, number, and special character."));
            }

            // 创建用户，密码加密存储
            User user = User.builder()
                    .uid(UUID.randomUUID().toString())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .roleType(roleType)
                    .libraryCode(libraryCode)
                    .build();

            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Registration failed: " + e.getMessage()));
        }
    }



    @GetMapping("/captcha")
    @Operation(summary = "生成图像验证码")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateCaptcha() {
        // 生成验证码文本和图片
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(150, 50, 4, 100);
        String code = captcha.getCode();
        String base64Img = captcha.getImageBase64();

        // 生成唯一ID作为key
        String captchaId = UUID.randomUUID().toString();

        // 存入Redis（5分钟有效）
        redisTemplate.opsForValue().set("captcha:" + captchaId, code, Duration.ofMinutes(5));

        // 返回给前端
        Map<String, String> result = Map.of(
                "captchaId", captchaId,
                "imgBase64", "data:image/png;base64," + base64Img
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 登录接口
     * 一般允许匿名访问，故去掉 @PreAuthorize 限制
     */
    @Operation(summary = "用户名密码登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getEmail();
        String libraryCode = loginRequest.getLibraryCode();
        String loginKeyPrefix = username + ":" + libraryCode;
        String failedKey = "login:fail:" + loginKeyPrefix;
        String lockKey = "login:lock:" + loginKeyPrefix;

        try {
            // ① 检查图像验证码
            String storedCode = redisTemplate.opsForValue().get("captcha:" + loginRequest.getCaptchaId());
            if (storedCode == null || !storedCode.equalsIgnoreCase(loginRequest.getCaptchaCode())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.failure("验证码错误或已过期"));
            }
            redisTemplate.delete("captcha:" + loginRequest.getCaptchaId());

            // ② 检查是否已锁定
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.failure("账号已锁定，请稍后再试"));
            }

            // ③ 尝试认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            // ④ 登录成功，清除失败记录
            redisTemplate.delete(failedKey);

            // ⑤ 生成 Token
            UserDetails userDetails = customUserDetailsServices.loadUserByUsernameAndLibraryCode(username, libraryCode);
            String jwt = jwtUtil.generateToken(userDetails, libraryCode, loginRequest.isRememberMe());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList();

            String key = username + libraryCode + "logintime";
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            redisTemplate.opsForValue().set(key, timestamp);

            String mainRole = roles.stream()
                    .map(r -> r.replace("ROLE_", "").toLowerCase())
                    .filter(r -> List.of("admin", "librarian", "reader").contains(r))
                    .min(Comparator.comparingInt(r -> List.of("admin", "librarian", "reader").indexOf(r)))
                    .orElse("reader");

            LoginResponse response = new LoginResponse(jwt, "Bearer", roles, mainRole);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            // ⑥ 登录失败：记录失败次数
            Long failedCount = redisTemplate.opsForValue().increment(failedKey);
            if (failedCount != null && failedCount >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.expire(lockKey, LOCK_TIME_SECONDS, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(lockKey, "LOCKED");
                redisTemplate.delete(failedKey);
            } else {
                redisTemplate.expire(failedKey, LOCK_TIME_SECONDS, TimeUnit.SECONDS);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("登录失败：" + e.getMessage()));
        }
    }




    /**
     * 管理员接口：获取所有角色列表
     * 只允许 ADMIN 访问
     */
    @Operation(summary = "管理员获取所有角色列表")
    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        List<String> roles = Arrays.stream(RoleType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
