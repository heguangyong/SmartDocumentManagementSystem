package com.github.sdms.config;

import com.github.sdms.dto.ThirdConfig;
import com.koalii.svs.client.Svs2ClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({ThirdConfig.class, SvsClientProperties.class, KmsProperties.class})
@RequiredArgsConstructor
public class CryptoAutoConfiguration {

    private final SvsClientProperties svsProps;

    @Bean
    @ConditionalOnProperty(prefix = "svs.service", name = "enabled", havingValue = "true")
    public Svs2ClientHelper svs2ClientHelper() {
        log.info("Init Svs2ClientHelper with host={}, port={}, timeout={}",
                svsProps.getHost(), svsProps.getPort(), svsProps.getTimeout());

        Svs2ClientHelper helper = Svs2ClientHelper.getInstance();
        helper.init(svsProps.getHost(), svsProps.getPort(), svsProps.getTimeout());
        return helper;
    }
}
