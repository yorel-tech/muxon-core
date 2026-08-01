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
package com.yorel.muxon;

import com.yorel.muxon.config.MuxonConsoleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MuxonConsoleProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.yorel.muxon.db.repository")
@EntityScan(basePackages = "com.yorel.muxon.db.model")
@ComponentScan(
    basePackages = {
      "com.yorel.muxon.config",
      "com.yorel.muxon.security",
      "com.yorel.muxon.controllers",
      "com.yorel.muxon.services",
      "com.yorel.muxon.events",
      "com.yorel.muxon.grpc",
      "com.yorel.muxon.info",
      "com.yorel.muxon.hateoas",
      "com.yorel.muxon.auth",
      "com.yorel.muxon.web",
      "com.yorel.muxon.db.resolver"
    })
public class CoreServicesApplication {
  public static void main(String[] args) {
    SpringApplication.run(CoreServicesApplication.class, args);
  }
}
