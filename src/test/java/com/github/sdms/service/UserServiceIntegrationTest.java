package com.github.sdms.service;

import com.github.sdms.config.SecurityTestConfig;
import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(SecurityTestConfig.class)
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateAndFindUser() {
        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setPassword("testpass");

        userService.saveUser(user);

        Optional<AppUser> savedUser = userRepository.findByUsernameAndLibraryCode("testuser","G123");
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.get().getUsername());
    }
}
