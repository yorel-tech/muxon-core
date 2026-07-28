package com.scal.muxon.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager(@Value("${authz.cache.ttl-seconds:300}") long ttlSeconds) {
        CaffeineCacheManager manager = new CaffeineCacheManager("userPermissions", "actionLinks");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
            .maximumSize(50_000));
        return manager;
    }

    /**
     * Tenant-id list per external user id; empty results use a shorter TTL (negative cache).
     */
    @Bean
    public Cache<String, List<String>> authzTenantCache(
            @Value("${authz.cache.ttl-seconds:300}") long ttlSeconds) {
        return Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfter(new Expiry<String, List<String>>() {
                @Override
                public long expireAfterCreate(String key, List<String> value, long currentTime) {
                    long sec = value.isEmpty() ? Math.min(ttlSeconds, 60) : ttlSeconds;
                    return TimeUnit.SECONDS.toNanos(sec);
                }

                @Override
                public long expireAfterUpdate(
                        String key,
                        List<String> value,
                        long currentTime,
                        long currentDuration) {
                    return expireAfterCreate(key, value, currentTime);
                }

                @Override
                public long expireAfterRead(
                        String key,
                        List<String> value,
                        long currentTime,
                        long currentDuration) {
                    return currentDuration;
                }
            })
            .build();
    }
}
