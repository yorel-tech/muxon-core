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
package com.yorel.muxon.customization.model;

/**
 * Lifecycle phases for guest customization. Stored as a string in {@code vms.customization_status}
 * JSONB.
 */
public enum CustomizationPhase {
  /** No customization configured for this VM. */
  NONE,
  /** Customization spec persisted; seed ISO not yet built. */
  PENDING,
  /** Seed ISO written to disk; VM not yet started. */
  SEED_BUILT,
  /** VM is running; waiting for QEMU guest agent to come online. */
  WAITING_AGENT,
  /** QEMU guest agent is reachable; cloud-init or sysprep is actively running. */
  IN_PROGRESS,
  /** All stages done; IP/hostname confirmed and stored; seed ISO cleaned up. */
  COMPLETE,
  /** Timeout exceeded or guest-reported error. */
  FAILED
}
