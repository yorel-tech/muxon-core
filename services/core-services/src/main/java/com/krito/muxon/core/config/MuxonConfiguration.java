package com.krito.muxon.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MuxonProperties.class)
public class MuxonConfiguration {
}
