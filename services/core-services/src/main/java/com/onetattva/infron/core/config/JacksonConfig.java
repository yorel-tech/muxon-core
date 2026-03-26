package com.onetattva.infron.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Jackson 3 has built-in Java 8 date/time support, no additional configuration needed
        return new ObjectMapper();
    }
}
