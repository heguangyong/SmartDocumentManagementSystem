package com.github.sdms.config;

import com.koalii.svs.client.Svs2ClientHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Svs2ClientHelperFactory {

    @Bean
    public Svs2ClientHelper svs2ClientHelper(SvsClientProperties props) {
        String[] servers = props.getAddrs().split(",");
        String[] addr = servers[0].split(":");
        String ip = addr[0].trim();
        int port = Integer.parseInt(addr[1].trim());

        Svs2ClientHelper helper = Svs2ClientHelper.getInstance();
        helper.init(ip, port, props.getTimeout());
        return helper;
    }
}

