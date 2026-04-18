package com.krito.muxon.core.config;

import com.krito.muxon.core.services.OidcUserService;
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
