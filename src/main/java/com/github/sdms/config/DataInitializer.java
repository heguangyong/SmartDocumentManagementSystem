package com.github.sdms.config;

import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
            createUserIfNotExists("admin", "admin_" + libraryCode + "@example.com", "admin123", Role.ADMIN, libraryCode);
            createUserIfNotExists("librarian", "librarian_" + libraryCode + "@example.com", "librarian123", Role.LIBRARIAN, libraryCode);
            createUserIfNotExists("reader", "reader_" + libraryCode + "@example.com", "reader123", Role.READER, libraryCode);
        }
    }

    /**
     * 如果用户不存在，则创建用户
     * @param username 用户名
     * @param email 用户邮箱
     * @param rawPassword 原始密码
     * @param role 用户角色
     * @param libraryCode 租户馆代码
     */
    private void createUserIfNotExists(String username, String email, String rawPassword, Role role, String libraryCode) {
        // 检查邮箱和用户名是否已存在
        if (!userRepository.existsByEmailAndLibraryCode(email, libraryCode) && !userRepository.existsByUsernameAndLibraryCode(username, libraryCode)) {
            AppUser user = AppUser.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .libraryCode(libraryCode)
                    .build();
            userRepository.save(user);
            System.out.printf("✅ [%s] 用户已创建: %s / %s / %s%n", role, email, rawPassword, libraryCode);
        } else {
            System.out.printf("ℹ️ [%s] 用户已存在: %s / %s%n", role, email, libraryCode);
        }
    }
}
