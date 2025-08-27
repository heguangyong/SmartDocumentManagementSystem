package com.github.sdms.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "third")
public class ThirdConfig {
    private String signCertId;
    private String signHost;
    private String hashType;
    private Integer signPort;
    private Integer signTimeOut;
}

