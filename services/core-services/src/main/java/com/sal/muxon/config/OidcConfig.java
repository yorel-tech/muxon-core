package com.sal.muxon.config;

import com.sal.muxon.services.OidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class OidcConfig {

    @Bean
    public OidcUserService oidcUserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new OidcUserService(restTemplate, objectMapper);
    }
}
