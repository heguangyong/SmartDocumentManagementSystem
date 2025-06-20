package com.Ayush.sdms_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SdmsBackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testPasswordEncoding() {
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String rawPassword = "testPassword123";
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		assertNotNull(encodedPassword);
		assertNotEquals(rawPassword, encodedPassword);
		assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
	}

}
