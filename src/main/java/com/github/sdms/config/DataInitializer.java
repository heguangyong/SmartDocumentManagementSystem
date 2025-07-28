package com.github.sdms.config;

import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initUsers() {
        // 初始化多个馆
        String[] libraryCodes = {"G123", "G456", "G789"}; // 假设有多个馆的 libraryCode

        // 对每个馆进行初始化
        for (String libraryCode : libraryCodes) {
            createUserIfNotExists("admin", "admin_" + libraryCode + "@example.com", "admin123", RoleType.ADMIN, libraryCode);
            createUserIfNotExists("librarian", "librarian_" + libraryCode + "@example.com", "librarian123", RoleType.LIBRARIAN, libraryCode);
            createUserIfNotExists("reader", "reader_" + libraryCode + "@example.com", "reader123", RoleType.READER, libraryCode);
        }
    }

    /**
     * 如果用户不存在，则创建用户
     * @param username 用户名
     * @param email 用户邮箱
     * @param rawPassword 原始密码
     * @param roleType 用户角色
     * @param libraryCode 租户馆代码
     */
    private void createUserIfNotExists(String username, String email, String rawPassword, RoleType roleType, String libraryCode) {
        // 检查邮箱和libraryCode是否已存在
        if (!userRepository.existsByEmailAndLibraryCode(email, libraryCode)) {
            // 生成唯一的uid
            String uid = UUID.randomUUID().toString();

            AppUser user = AppUser.builder()
                    .uid(uid)  // 使用唯一的uid
                    .username(username)  // username不做唯一性检查
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .roleType(roleType)
                    .libraryCode(libraryCode)
                    .build();

            userRepository.save(user);
            System.out.printf("✅ [%s] 用户已创建: %s / %s / %s / %s%n", roleType, email, rawPassword, libraryCode, uid);
        } else {
            System.out.printf("ℹ️ [%s] 用户已存在: %s / %s%n", roleType, email, libraryCode);
        }
    }
}
