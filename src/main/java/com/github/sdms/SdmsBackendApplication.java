package com.github.sdms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ 启用定时任务调度支持
public class SdmsBackendApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SdmsBackendApplication.class, args);

		// 从 Spring 环境获取实际端口
		Environment env = context.getEnvironment();
		String port = env.getProperty("server.port", "8090");

		System.out.println("🚀 SDMS Backend started successfully!");
		System.out.println("📡 API available at: http://localhost:" + port);
	}
}

