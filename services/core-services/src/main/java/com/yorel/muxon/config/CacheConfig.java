/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
    manager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(ttlSeconds)).maximumSize(50_000));
    return manager;
  }

  /** Tenant-id list per external user id; empty results use a shorter TTL (negative cache). */
  @Bean
  public Cache<String, List<String>> authzTenantCache(
      @Value("${authz.cache.ttl-seconds:300}") long ttlSeconds) {
    return Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfter(
            new Expiry<String, List<String>>() {
              @Override
              public long expireAfterCreate(String key, List<String> value, long currentTime) {
                long sec = value.isEmpty() ? Math.min(ttlSeconds, 60) : ttlSeconds;
                return TimeUnit.SECONDS.toNanos(sec);
              }

              @Override
              public long expireAfterUpdate(
                  String key, List<String> value, long currentTime, long currentDuration) {
                return expireAfterCreate(key, value, currentTime);
              }

              @Override
              public long expireAfterRead(
                  String key, List<String> value, long currentTime, long currentDuration) {
                return currentDuration;
              }
            })
        .build();
  }
}
