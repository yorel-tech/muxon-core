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
package com.yorel.muxon.db.model;

import com.yorel.muxon.api.enums.BootstrapStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing system initialization status. Tracks the bootstrap lifecycle state in the
 * system_init table.
 */
@Entity
@Table(name = "system_init")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemInitEntity {

  /**
   * Primary key for the system_init table entry. Examples: "bootstrap_status", "BOOTSTRAP_DONE_KEY"
   */
  @Id
  @Column(name = "primary_key", nullable = false, length = 100)
  private String primaryKey;

  /** Value associated with the primary key. For bootstrap_status: true/false (legacy support) */
  @Column(name = "value", columnDefinition = "text")
  private String value;

  /**
   * System status for tracking bootstrap lifecycle. Uses the three-state lifecycle: NOTREADY ->
   * BOOTSTRAPPED -> READY
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "system_status", nullable = false)
  private BootstrapStatus systemStatus;

  /** Timestamp when this entry was last updated. */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Creates a new SystemInitEntity with the primary key and a boolean value. Used for legacy
   * support with BOOTSTRAP_DONE_KEY.
   */
  public static SystemInitEntity createWithBoolean(String primaryKey, boolean value) {
    return new SystemInitEntity(
        primaryKey, value ? "true" : "false", BootstrapStatus.NOTREADY, LocalDateTime.now());
  }

  /** Creates a new SystemInitEntity with the primary key, value, and system status. */
  public static SystemInitEntity create(String primaryKey, String value) {
    return new SystemInitEntity(primaryKey, value, BootstrapStatus.NOTREADY, LocalDateTime.now());
  }

  /** Creates a new SystemInitEntity with the primary key, value, and system status. */
  public static SystemInitEntity create(
      String primaryKey, String value, BootstrapStatus systemStatus) {
    return new SystemInitEntity(primaryKey, value, systemStatus, LocalDateTime.now());
  }
}
