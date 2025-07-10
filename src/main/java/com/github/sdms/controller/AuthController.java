package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.components.JwtUtil;
import com.github.sdms.dto.RegisterRequest;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.CustomUserDetailsServices;
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

    // 多角色（如管理员或用户都可访问）
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    // 用户权限
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("Email already exists"));
            }

            AppUser user = AppUser.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.valueOf(request.getRole()))
                    .build();

            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Registration failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")     // ✅ 正确方式
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestParam String email, @RequestParam String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            UserDetails userDetails = customUserDetailsServices.loadUserByUsername(email);
            String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(ApiResponse.success(jwt));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Login failed ❌: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        List<String> roles = Arrays.stream(Role.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }


}