package com.github.sdms.config;

import com.github.sdms.model.User;
import com.github.sdms.model.LibrarySite;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.repository.LibrarySiteRepository;
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
    private final LibrarySiteRepository librarySiteRepository;

    @PostConstruct
    public void initUsers() {
        // 创建默认馆点
        LibrarySite defaultLibrarySite = createDefaultLibrarySite();

        // 仅创建默认管理员账号，并赋值馆点
        createUserIfNotExists("admin", "admin@example.com", "admin123", RoleType.ADMIN, defaultLibrarySite.getCode());
    }

    /**
     * 创建默认馆点并返回
     * @return 默认馆点
     */
    private LibrarySite createDefaultLibrarySite() {
        // 检查默认馆点是否已存在
        String defaultLibraryCode = "DEFAULT";
        if (!librarySiteRepository.existsByCode(defaultLibraryCode)) {
            LibrarySite librarySite = LibrarySite.builder()
                    .code(defaultLibraryCode)
                    .name("默认馆点")
                    .address("默认地址")
                    .type("主馆")
                    .status(true)
                    .build();

            librarySiteRepository.save(librarySite);
            System.out.println("✅ 默认馆点已创建: " + librarySite.getCode());
            return librarySite;
        } else {
            System.out.println("ℹ️ 默认馆点已存在");
            return librarySiteRepository.findByCode(defaultLibraryCode)
                    .orElseThrow(() -> new RuntimeException("默认馆点不存在"));
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
        // 检查用户名是否已存在
        if (!userRepository.existsByUsername(username)) {
            // 生成唯一的uid
            String uid = UUID.randomUUID().toString();

            User user = User.builder()
                    .uid(uid)  // 使用唯一的uid
                    .username(username)  // username做唯一性检查
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .roleType(roleType)
                    .libraryCode(libraryCode)  // 赋值默认馆点code给管理员
                    .build();

            userRepository.save(user);
            System.out.printf("✅ [管理员] 用户已创建: %s / %s / %s%n", username, rawPassword, uid);
        } else {
            System.out.printf("ℹ️ [管理员] 用户已存在: %s%n", username);
        }
    }
}
