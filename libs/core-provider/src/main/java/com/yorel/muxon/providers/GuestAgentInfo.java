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
package com.yorel.muxon.providers;

import java.util.List;

/**
 * Information returned by the QEMU guest agent (QGA) channel. Used for post-boot telemetry: IP
 * addresses, hostname, and customization phase.
 */
public record GuestAgentInfo(
    List<String> ipAddresses,
    String hostname,
    /** Raw cloud-init status JSON string (Linux only; null on Windows). */
    String cloudInitStatusJson,
    /** True when QGA responded; false when the channel is not yet reachable. */
    boolean agentReachable) {
  public static GuestAgentInfo unreachable() {
    return new GuestAgentInfo(List.of(), null, null, false);
  }
}
