package com.github.sdms.service;

import com.github.sdms.config.SecurityTestConfig;
import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.RoleType;
import com.github.sdms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(SecurityTestConfig.class)
@Transactional
public class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void testFindByEmail() {
        AppUser user = AppUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encryptedPwd")
                .roleType(RoleType.READER)
                .libraryCode("123456")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<AppUser> foundUser = userService.findByEmailAndLibraryCode("test@example.com","123456");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    void testExistsByEmailAndLibraryCode() {
        // 假设你的 Repository 使用的是 `existsByEmailAndLibraryCode` 方法
        String email = "test@example.com";
        String libraryCode = "libraryCode123";

        when(userRepository.existsByEmailAndLibraryCode(email, libraryCode)).thenReturn(true);

        boolean exists = userService.existsByEmailAndLibraryCode(email, libraryCode);
        assertTrue(exists);
    }

    @Test
    void testSaveUser() {
        AppUser user = AppUser.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("pwd")
                .roleType(RoleType.ADMIN)
                .build();

        when(userRepository.save(user)).thenReturn(user);

        AppUser savedUser = userService.saveUser(user);
        assertNotNull(savedUser);
        assertEquals("newuser", savedUser.getUsername());
    }

    @Test
    void testDeleteUser() {
        long userId = 1L;
        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }
}
