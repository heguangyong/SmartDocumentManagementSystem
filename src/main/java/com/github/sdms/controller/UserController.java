package com.github.sdms.controller;

import com.github.sdms.dto.ApiResponse;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.MinioService;
import com.github.sdms.service.ShareAccessLogService;
import com.github.sdms.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
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
    private final MinioService minioService;
    private final JwtUtil jwtUtil;
    @Resource
    private final ShareAccessLogService shareAccessLogService;


    @Operation(summary = "创建用户", description = "创建新用户，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AppUser> createUser(@RequestBody AppUser user, @RequestParam String libraryCode) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "获取所有用户列表", description = "获取所有用户列表，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<List<AppUser>> getAllUsers(@RequestParam String libraryCode) {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Operation(summary = "删除用户", description = "根据用户ID删除用户，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, @RequestParam String libraryCode) {
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    @Operation(summary = "获取用户详情", description = "获取指定ID的用户详情，当前用户或管理员可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id, Authentication authentication, @RequestParam String libraryCode) {
        AppUser currentUser = userRepository.findByEmailAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
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

    @Operation(summary = "清理上传缓存", description = "管理员清理上传缓存，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cleanup/cache")
    public ResponseEntity<ApiResponse<String>> clearUploadCache(@RequestParam String libraryCode) {
        boolean success = minioService.clearUploadCache();
        return ResponseEntity.ok(ApiResponse.success(success ? "Upload cache cleared." : "No cache to clear."));
    }

    @Operation(summary = "获取当前用户上传的文件列表", description = "获取当前登录用户上传的文件列表，读者及以上权限可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/files/my")
    public ResponseEntity<ApiResponse<List<String>>> listMyFiles(Authentication authentication, @RequestParam String libraryCode) {
        String username = authentication.getName();
        List<String> files = minioService.getUserFiles(username);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @Operation(summary = "获取当前登录用户基本信息", description = "获取当前登录用户的基本信息，读者及以上权限可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/info/summary")
    public ResponseEntity<ApiResponse<AppUser>> getCurrentUserInfo(Authentication authentication, @RequestParam String libraryCode) {
        AppUser user = userRepository.findByEmailAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "修改当前用户用户名", description = "修改当前登录用户的用户名，读者及以上权限可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @PutMapping("/me/username")
    public ResponseEntity<?> updateUsername(@RequestParam String newUsername, Authentication authentication, @RequestParam String libraryCode) {
        Optional<AppUser> userOpt = userRepository.findByEmailAndLibraryCode(authentication.getName(), libraryCode);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            user.setUsername(newUsername);
            userRepository.save(user);
            return ResponseEntity.ok("Username updated");
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "重置用户密码", description = "重置指定用户的密码，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String newPassword, @RequestParam String libraryCode) {
        Optional<AppUser> userOpt = userRepository.findByEmailAndLibraryCode(email, libraryCode);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok("Password reset");
        }
        return ResponseEntity.badRequest().body("User not found");
    }

    @Operation(summary = "为用户分配角色", description = "管理员为用户分配角色")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/role") // 去掉 '/user'，与控制器的前缀配合
    public ResponseEntity<String> assignRoleToUser(@PathVariable Long userId, @RequestBody String role) {
        // 获取目标用户
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "User not found"));

        // 判断角色是否合法（LIBRARIAN 或 READER 或 ADMIN）
        if (!role.equals("LIBRARIAN") && !role.equals("READER") && !role.equals("ADMIN")) {
            throw new ApiException(400, "Invalid role");
        }

        // 分配角色
        user.setRole(Role.valueOf(role));
        userRepository.save(user);

        return ResponseEntity.ok("Role " + role + " assigned to user successfully.");
    }


    @Operation(summary = "管理员权限测试接口", description = "仅管理员权限")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/ping")
    public ResponseEntity<ApiResponse<String>> adminPing() {
        return ResponseEntity.ok(ApiResponse.success("✅ ADMIN 权限验证成功！"));
    }

    @Operation(summary = "查询所有分享访问日志", description = "仅管理员可查看")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/share-access-logs")
    public ResponseEntity<ApiResponse<?>> getShareAccessLogs() {
        return ResponseEntity.ok(ApiResponse.success("查询成功", shareAccessLogService.getAllLogs()));
    }
}
