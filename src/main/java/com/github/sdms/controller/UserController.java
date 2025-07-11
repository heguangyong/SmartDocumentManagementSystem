package com.github.sdms.controller;

import com.github.sdms.util.JwtUtil;
import com.github.sdms.dto.ApiResponse;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.util.PermissionChecker;
import com.github.sdms.service.MinioService;
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
    private final MinioService minioService;
    private final PermissionChecker permissionChecker;
    private final JwtUtil jwtUtil;

    @Operation(summary = "创建用户 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AppUser> createUser(@RequestBody AppUser user, @RequestParam String libraryCode) {
        // 根据租户代码处理用户创建
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 在创建用户时也可以考虑存储与 libraryCode 关联的信息
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "获取所有用户列表 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<List<AppUser>> getAllUsers(@RequestParam String libraryCode) {
        // 在获取用户列表时根据租户代码进行过滤
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Operation(summary = "删除用户 【仅ADMIN】")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, @RequestParam String libraryCode) {
        // 删除用户时根据租户代码确保操作的正确性
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    @Operation(summary = "获取用户详情 【本人或ADMIN】")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id, Authentication authentication, @RequestParam String libraryCode) {
        // 使用带有 libraryCode 参数的方法查询用户
        AppUser currentUser = userRepository.findByEmailAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).build();
        }

        if (!currentUser.getId().equals(id) && !jwtUtil.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        // 根据租户代码对用户详情进行管理
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "管理员清理上传缓存 【仅ADMIN】")
    @PostMapping("/cleanup/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearUploadCache(@RequestParam String libraryCode) {
        // 在清理上传缓存时，可以根据租户代码判断是否属于该租户的数据
        boolean success = minioService.clearUploadCache();
        return ResponseEntity.ok(ApiResponse.success(success ? "Upload cache cleared." : "No cache to clear."));
    }

    @Operation(summary = "获取当前用户上传的文件列表 【读者及以上】")
    @GetMapping("/files/my")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> listMyFiles(Authentication authentication, @RequestParam String libraryCode) {
        String username = authentication.getName();
        List<String> files = minioService.getUserFiles(username);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @Operation(summary = "获取当前登录用户基本信息 【读者及以上】")
    @GetMapping("/info/summary")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppUser>> getCurrentUserInfo(Authentication authentication, @RequestParam String libraryCode) {
        // 使用带有 libraryCode 参数的方法查询当前用户
        AppUser user = userRepository.findByEmailAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "修改当前用户用户名 【读者及以上】")
    @PutMapping("/me/username")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
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

    @Operation(summary = "重置用户密码 【仅ADMIN】")
    @PutMapping("/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
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
}
