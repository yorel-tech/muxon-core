package com.onetattva.infron.core.config;

import tools.jackson.databind.ObjectMapper;
import com.onetattva.infron.core.services.OidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OidcConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Jackson 3 has built-in Java 8 date/time support, no module registration needed
        return new ObjectMapper();
    }

    @Bean
    public OidcUserService oidcUserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new OidcUserService(restTemplate, objectMapper);
    }
}
