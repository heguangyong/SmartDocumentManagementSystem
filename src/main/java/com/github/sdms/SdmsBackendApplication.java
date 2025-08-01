package com.github.sdms;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ 启用定时任务调度支持
public class SdmsBackendApplication {

	public static void main(String[] args) {
		// Load environment variables from .env file
		Dotenv dotenv = Dotenv.configure()
				.directory("./")
				.filename(".env")
				.ignoreIfMissing()
				.load();
		
		// Set system properties for Spring Boot to use
		dotenv.entries().forEach(entry -> 
			System.setProperty(entry.getKey(), entry.getValue())
		);
		
		SpringApplication.run(SdmsBackendApplication.class, args);
		System.out.println("🚀 SDMS Backend started successfully!");
		System.out.println("📡 API available at: http://localhost:" + 
			(System.getProperty("SERVER_PORT") != null ? System.getProperty("SERVER_PORT") : "8080"));
	}
}
