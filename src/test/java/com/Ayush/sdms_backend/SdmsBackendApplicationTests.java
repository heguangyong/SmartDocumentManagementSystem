package com.Ayush.sdms_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SdmsBackendApplicationTests {

	@Autowired
	private S3Client s3Client;

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

	@Test
	void testMinioConnection() {
		List<Bucket> buckets = s3Client.listBuckets().buckets();
		System.out.println("Buckets in MinIO:");
		for (Bucket bucket : buckets) {
			System.out.println(" - " + bucket.name());
		}

		// 添加断言：确保至少存在一个桶（即你创建的 sdmsfilesmanager）
		assertTrue(buckets.stream().anyMatch(b -> b.name().equals("sdmsfilesmanager")));
	}

}
