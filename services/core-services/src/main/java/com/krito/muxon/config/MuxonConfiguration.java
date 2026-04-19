package com.krito.muxon.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MuxonProperties.class)
public class MuxonConfiguration {
}
