package com.github.sdms.service;

import com.github.sdms.model.AppUser;
import com.github.sdms.model.enums.Role;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void testFindByEmail() {
        AppUser user = AppUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encryptedPwd")
                .role(Role.valueOf("USER"))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<AppUser> foundUser = userService.findByEmail("test@example.com");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    void testExistsByEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        boolean exists = userService.existsByEmail("test@example.com");
        assertTrue(exists);
    }

    @Test
    void testSaveUser() {
        AppUser user = AppUser.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("pwd")
                .role(Role.valueOf("ADMIN"))
                .build();

        when(userRepository.save(user)).thenReturn(user);

        AppUser savedUser = userService.saveUser(user);
        assertNotNull(savedUser);
        assertEquals("newuser", savedUser.getUsername());
    }

    @Test
    void testDeleteUser() {
        doNothing().when(userRepository).deleteById(1L);
        userService.deleteUser(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }
}
