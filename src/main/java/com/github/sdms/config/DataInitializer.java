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
        createUserIfNotExists("admin", "admin@example.com", "admin123", Role.ADMIN);
        createUserIfNotExists("librarian", "librarian@example.com", "librarian123", Role.LIBRARIAN);
        createUserIfNotExists("reader", "reader@example.com", "reader123", Role.READER);
    }

    private void createUserIfNotExists(String username, String email, String rawPassword, Role role) {
        if (!userRepository.existsByEmail(email)) {
            AppUser user = AppUser.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .build();
            userRepository.save(user);
            System.out.printf("✅ [%s] 用户已创建: %s / %s%n", role, email, rawPassword);
        } else {
            System.out.printf("ℹ️ [%s] 用户已存在: %s%n", role, email);
        }
    }
}
