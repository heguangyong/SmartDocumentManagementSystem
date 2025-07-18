package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.util.JwtUtil;
import com.github.sdms.dto.LoginResponse;
import com.github.sdms.dto.RegisterRequest;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.CustomUserDetailsServices;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    /**
     * 任何已登录用户都可以访问此接口，用于测试权限
     * 允许所有角色访问：READER、LIBRARIAN、ADMIN
     */
    @Operation(summary = "测试权限接口【权限：读者及以上】")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    /**
     * 注册接口
     * 一般允许匿名访问或仅ADMIN访问（这里开放匿名，若需可加 @PreAuthorize("hasRole('ADMIN')")）
     */
    @Operation(summary = "用户注册接口【权限：匿名访问】")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        try {
            // 验证 email 和 libraryCode 组合是否已存在
            if (userRepository.existsByEmailAndLibraryCode(request.getEmail(), request.getLibraryCode())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Email already exists for this libraryCode"));
            }

            // 校验传入角色是否有效（READER, LIBRARIAN, ADMIN）
            Role role;
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid role: " + request.getRole()));
            }

            // 获取租户信息 libraryCode
            String libraryCode = request.getLibraryCode();
            if (libraryCode == null || libraryCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("libraryCode must not be empty"));
            }
            // 创建用户对象并保存到数据库
            AppUser user = AppUser.builder()
                    // 生成唯一的uid
                    .uid(UUID.randomUUID().toString())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .libraryCode(libraryCode)  // 保存 libraryCode 字段
                    .build();

            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Registration failed: " + e.getMessage()));
        }
    }




    /**
     * 登录接口
     * 一般允许匿名访问，故去掉 @PreAuthorize 限制
     */
    @Operation(summary = "用户登录接口【权限：匿名访问】")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestParam String email, @RequestParam String password, @RequestParam String libraryCode) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            UserDetails userDetails = customUserDetailsServices.loadUserByUsernameAndLibraryCode(email, libraryCode);
            String jwt = jwtUtil.generateToken(userDetails, libraryCode);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList();

            // ✅ 写入 Redis 登录时间戳
            String key = userDetails.getUsername() + libraryCode + "logintime";
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            redisTemplate.opsForValue().set(key, timestamp);

            // 生成主角色（iss）字段，剥离 ROLE_ 前缀后参与排序
            String mainRole = roles.stream()
                    .map(r -> r.replace("ROLE_", "").toLowerCase())
                    .filter(r -> List.of("admin", "librarian", "reader").contains(r))
                    .min(Comparator.comparingInt(r -> List.of("admin", "librarian", "reader").indexOf(r)))
                    .orElse("reader");
            // ✅ 构建响应体
            LoginResponse response = new LoginResponse(jwt, "Bearer", roles, mainRole);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Login failed ❌: " + e.getMessage()));
        }
    }


    /**
     * 管理员接口：获取所有角色列表
     * 只允许 ADMIN 访问
     */
    @Operation(summary = "获取所有角色列表【权限：仅管理员】")
    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        List<String> roles = Arrays.stream(Role.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
