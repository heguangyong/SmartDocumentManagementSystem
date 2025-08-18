package com.github.sdms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kms")
public class KmsProperties {
    private boolean enabled;
    private String mode = "CFB";
    private String iv; // Base64
    private String configPath = "classpath:kms_client.yml";
}