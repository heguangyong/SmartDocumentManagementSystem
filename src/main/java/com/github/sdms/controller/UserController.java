package com.github.sdms.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.github.sdms.dto.*;
import com.github.sdms.exception.ApiException;
import com.github.sdms.model.User;
import com.github.sdms.model.enums.AuditActionType;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.*;
import com.github.sdms.util.JwtUtil;
import com.github.sdms.util.PasswordUtil;
import com.github.sdms.util.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsServices customUserDetailsServices;
    private final MinioService minioService;
    private final ShareAccessLogService shareAccessLogService;
    private final OAuthUserInfoService oauthUserInfoService;
    private final UserAuditLogService userAuditLogService;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_SECONDS = 30 * 60;


    // ====== Auth 原逻辑整合 START ======

    @Operation(summary = "图像验证码生成")
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(150, 50, 4, 100);
        String code = captcha.getCode();
        String base64Img = captcha.getImageBase64();
        String captchaId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("captcha:" + captchaId, code, Duration.ofMinutes(5));
        Map<String, String> result = Map.of("captchaId", captchaId, "imgBase64", "data:image/png;base64," + base64Img);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername(); // ✅ 替换获取方式
        String libraryCode = loginRequest.getLibraryCode();
        String loginKeyPrefix = username + ":" + libraryCode;
        String failedKey = "login:fail:" + loginKeyPrefix;
        String lockKey = "login:lock:" + loginKeyPrefix;

        String ip = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);

        try {
            String storedCode = redisTemplate.opsForValue().get("captcha:" + loginRequest.getCaptchaId());
            if (storedCode == null || !storedCode.equalsIgnoreCase(loginRequest.getCaptchaCode())) {
                return ResponseEntity.badRequest().body(ApiResponse.failure("验证码错误或已过期"));
            }
            redisTemplate.delete("captcha:" + loginRequest.getCaptchaId());

            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure("账号已锁定，请稍后再试"));
            }

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
            redisTemplate.delete(failedKey);

            UserDetails userDetails = customUserDetailsServices.loadUserByUsernameAndLibraryCode(username, libraryCode);

            // 登录成功后：
            User user = userRepository.findByUsernameAndLibraryCode(username, libraryCode)
                    .orElseThrow(() -> new ApiException(404, "User not found"));

            // 记录审计日志（登录成功）
            userAuditLogService.log(user.getId(), username, libraryCode, ip, userAgent, AuditActionType.LOGIN_SUCCESS, "登录成功");

            if (Boolean.TRUE.equals(user.getNeedPasswordChange())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure("初次登录需修改密码"));
            }

            String jwt = jwtUtil.generateToken(userDetails, libraryCode, loginRequest.isRememberMe());
            List<String> roles = userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()).toList();
            redisTemplate.opsForValue().set(username + libraryCode + "logintime", String.valueOf(System.currentTimeMillis() / 1000));

            String mainRole = roles.stream().map(r -> r.replace("ROLE_", "").toLowerCase())
                    .filter(r -> List.of("admin", "librarian", "reader").contains(r))
                    .min(Comparator.comparingInt(r -> List.of("admin", "librarian", "reader").indexOf(r)))
                    .orElse("reader");

            return ResponseEntity.ok(ApiResponse.success(new LoginResponse(jwt, "Bearer", roles, mainRole)));

        } catch (Exception e) {
            // 登录失败审计日志
            userAuditLogService.log(null, username, loginRequest.getLibraryCode(), ip, userAgent, AuditActionType.LOGIN_FAIL, "登录失败：" + e.getMessage());

            Long failedCount = redisTemplate.opsForValue().increment(failedKey);
            if (failedCount != null && failedCount >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TIME_SECONDS, TimeUnit.SECONDS);
                redisTemplate.delete(failedKey);
            } else {
                redisTemplate.expire(failedKey, LOCK_TIME_SECONDS, TimeUnit.SECONDS);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure("登录失败：" + e.getMessage()));
        }
    }


    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody ChangePasswordRequest req,
            Authentication authentication,
            HttpServletRequest request) {

        String libraryCode = jwtUtil.getCurrentLibraryCode();
        if (libraryCode == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("无法识别libraryCode"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsernameAndLibraryCode(username, libraryCode)
                .orElseThrow(() -> new ApiException(404, "User not found"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("旧密码不正确"));
        }
        if (!PasswordUtil.isStrongPassword(req.getNewPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("新密码不符合复杂度要求"));
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setNeedPasswordChange(false);
        userRepository.save(user);

        // ✅ 审计日志：密码修改
        String ip = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);
        userAuditLogService.log(user.getId(), username, libraryCode, ip, userAgent, AuditActionType.PASSWORD_CHANGE, "用户修改密码");

        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
    }


    @Operation(summary = "注册馆员")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsernameAndLibraryCode(request.getUsername(), request.getLibraryCode())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Username already exists for this libraryCode"));
        }

        RoleType roleType;
        try {
            roleType = RoleType.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid role: " + request.getRole()));
        }

        if (!PasswordUtil.isStrongPassword(request.getPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Password must be at least 8 characters and include uppercase, lowercase, number, and special character."));
        }

        User user = User.builder().uid(UUID.randomUUID().toString()).username(request.getUsername()).email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).roleType(roleType).libraryCode(request.getLibraryCode()).build();

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
    }

    @Operation(summary = "获取所有角色")
    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllRoles() {
        List<String> roles = Arrays.stream(RoleType.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @Operation(summary = "测试权限接口")
    @GetMapping("/test")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }


    @Operation(summary = "创建用户", description = "创建新用户，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody User user, @RequestParam String libraryCode) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "获取所有用户列表", description = "获取所有用户列表，仅管理员可执行。")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers(@RequestParam String libraryCode) {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Operation(summary = "分页查询缓存用户信息", description = "管理员分页查询缓存中的 OAuth 用户信息")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cached")
    public ResponseEntity<ApiResponse<PagedResult<UserInfo>>> getCachedUsers(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        PagedResult<UserInfo> result = oauthUserInfoService.searchUsersPaged(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }


    @Operation(summary = "删除用户", description = "管理员删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")  // 路径参数由 uid 改为 id
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        // 根据 id 查找用户
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "User not found"));

        // 检查桶或文件依赖的逻辑请自行补充

        // 删除用户
        userRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.success("User with id " + id + " has been deleted."));
    }



    @Operation(summary = "获取用户详情", description = "获取指定ID的用户详情，当前用户或管理员可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id, Authentication authentication, @RequestParam String libraryCode) {
        User currentUser = userRepository.findByUsernameAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).build();
        }

        if (!currentUser.getId().equals(id) && !jwtUtil.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        return userRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<ApiResponse<User>> getCurrentUserInfo(Authentication authentication, @RequestParam String libraryCode) {
        User user = userRepository.findByUsernameAndLibraryCode(authentication.getName(), libraryCode).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "修改当前用户用户名", description = "修改当前登录用户的用户名，读者及以上权限可执行。")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @PutMapping("/me/username")
    public ResponseEntity<ApiResponse<String>> updateUsername(@RequestParam String newUsername, Authentication authentication, @RequestParam String libraryCode) {
        Optional<User> userOpt = userRepository.findByUsernameAndLibraryCode(authentication.getName(), libraryCode);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(newUsername);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success("Username updated"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure("User not found"));
    }

    @Operation(summary = "重置用户密码", description = "管理员重置指定用户的密码")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestParam String username, @RequestParam String newPassword, @RequestParam String libraryCode) {
        // 根据 email 和 libraryCode 查找用户
        User user = userRepository.findByUsernameAndLibraryCode(username, libraryCode).orElseThrow(() -> new ApiException(404, "User not found"));

        // 验证新密码是否为空或不符合密码规则（可选）
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ApiException(400, "Password cannot be empty");
        }

        // 重置密码并保存
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully for user with username " + username));
    }


    @Operation(summary = "为用户分配角色", description = "管理员为用户分配角色")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/role") // 路径参数改为 id
    public ResponseEntity<ApiResponse<String>> assignRoleToUser(@PathVariable Long id, @RequestBody String role) {
        // 根据 id 查找用户
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "User not found"));

        // 判断角色是否合法
        if (!role.equals("LIBRARIAN") && !role.equals("READER") && !role.equals("ADMIN")) {
            throw new ApiException(400, "Invalid role");
        }

        // 分配角色
        user.setRoleType(RoleType.valueOf(role));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Role " + role + " assigned to user successfully."));
    }



    @Operation(summary = "查询所有分享访问日志", description = "仅管理员可查看")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/share-access-logs")
    public ResponseEntity<ApiResponse<?>> getShareAccessLogs() {
        return ResponseEntity.ok(ApiResponse.success("查询成功", shareAccessLogService.getAllLogs()));
    }
}
