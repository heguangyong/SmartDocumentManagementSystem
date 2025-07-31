package com.github.sdms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "svs.service")
public class SvsClientProperties {
    private String addrs;     // 多个地址逗号分隔
    private String name;
    private int timeout = 20000; // 默认超时
}
