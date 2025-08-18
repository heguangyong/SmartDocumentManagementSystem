package com.github.sdms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "svs.service")
public class SvsClientProperties {
    private boolean enabled;
    private String host;
    private int port;
    private int timeout = 20000;
    private String certId;
}
