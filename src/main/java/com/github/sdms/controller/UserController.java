package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.MinioClientService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioClientService minioClientService;

    // 仅 ADMIN 可创建用户
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AppUser> createUser(@RequestBody AppUser user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    // 所有人可查看用户列表
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<AppUser>> getAllUser(){
        return ResponseEntity.of(Optional.of(userRepository.findAll()));
    }

    // ADMIN 可删除用户
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    // ADMIN/USER 可查看个人详情（可扩展为根据 uid 控制）
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "管理员清理上传缓存")
    @PostMapping("/cleanup/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearUploadCache() {
        // 调用 Minio 清理方法（模拟）
        boolean success = minioClientService.clearUploadCache();
        return ResponseEntity.ok(ApiResponse.success(success ? "Upload cache cleared." : "No cache to clear."));
    }

    @Operation(summary = "获取当前用户上传的文件列表")
    @GetMapping("/files/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> listMyFiles(Principal principal) {
        String username = principal.getName();
        List<String> files = minioClientService.getUserFiles(username);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @Operation(summary = "查看当前登录用户基本信息")
    @GetMapping("/info/summary")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AppUser>> getCurrentUserInfo(Principal principal) {
        AppUser user = userRepository.findByEmail(principal.getName()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AppUser> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUser>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }


    @PutMapping("/me/username")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> updateUsername(@RequestParam String newUsername, Authentication authentication) {
        String email = authentication.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            user.setUsername(newUsername);
            userRepository.save(user);
            return ResponseEntity.ok("Username updated");
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok("Password reset");
        }
        return ResponseEntity.badRequest().body("User not found");
    }


}
