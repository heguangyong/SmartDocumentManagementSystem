package com.github.sdms.controller;

import com.github.sdms.util.JwtUtil;
import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.util.PermissionChecker;
import com.github.sdms.service.MinioClientService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioClientService minioClientService;
    private final PermissionChecker permissionChecker;
    private final JwtUtil jwtUtil;


    @Operation(summary = "创建用户 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AppUser> createUser(@RequestBody AppUser user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "获取所有用户列表 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<List<AppUser>> getAllUsers(){
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Operation(summary = "删除用户 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    @Operation(summary = "获取用户详情 【本人或ADMIN】")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id, Authentication authentication) {
        AppUser currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).build();
        }

        if (!currentUser.getId().equals(id) && !jwtUtil.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "管理员清理上传缓存 【仅ADMIN】")
    @PostMapping("/cleanup/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearUploadCache() {
        boolean success = minioClientService.clearUploadCache();
        return ResponseEntity.ok(ApiResponse.success(success ? "Upload cache cleared." : "No cache to clear."));
    }

    @Operation(summary = "获取当前用户上传的文件列表 【读者及以上】")
    @GetMapping("/files/my")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> listMyFiles(Authentication authentication) {
        String username = authentication.getName();
        List<String> files = minioClientService.getUserFiles(username);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @Operation(summary = "获取当前登录用户基本信息 【读者及以上】")
    @GetMapping("/info/summary")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppUser>> getCurrentUserInfo(Authentication authentication) {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "修改当前用户用户名 【读者及以上】")
    @PutMapping("/me/username")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<?> updateUsername(@RequestParam String newUsername, Authentication authentication) {
        Optional<AppUser> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            user.setUsername(newUsername);
            userRepository.save(user);
            return ResponseEntity.ok("Username updated");
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "重置用户密码 【仅ADMIN】")
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
