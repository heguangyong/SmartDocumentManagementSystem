package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.components.JwtUtil;
import com.github.sdms.dto.LoginResponse;
import com.github.sdms.dto.RegisterRequest;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.CustomUserDetailsServices;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@RestController
@RequestMapping("/auth/local")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsServices customUserDetailsServices;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Email already exists"));
            }

            // 校验传入角色是否有效（READER, LIBRARIAN, ADMIN）
            Role role;
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid role: " + request.getRole()));
            }

            AppUser user = AppUser.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestParam String email, @RequestParam String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            UserDetails userDetails = customUserDetailsServices.loadUserByUsername(email);
            String jwt = jwtUtil.generateToken(userDetails);

            // 获取所有角色字符串列表
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())  // 例如 ROLE_ADMIN
                    .toList();

            LoginResponse response = new LoginResponse(jwt, "Bearer", roles);

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
