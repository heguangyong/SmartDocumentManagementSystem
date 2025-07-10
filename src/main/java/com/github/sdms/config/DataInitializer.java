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
    public void initAdminUser() {
        String email = "admin@example.com";

        if (!userRepository.existsByEmail(email)) {
            AppUser admin = AppUser.builder()
                    .username("admin")
                    .email(email)
                    .password(passwordEncoder.encode("admin123")) // 设定初始密码
                    .role(Role.valueOf("ADMIN"))
                    .build();

            userRepository.save(admin);
            System.out.println("✅ ADMIN 用户已创建: admin@example.com / admin123");
        } else {
            System.out.println("ℹ️ ADMIN 用户已存在");
        }
    }
}
