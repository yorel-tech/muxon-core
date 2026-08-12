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

/**
 * Represents a seed ISO artifact to be attached to a VM as a CD-ROM for guest customization.
 *
 * @param isoPath Absolute path to the seed ISO on the provider host filesystem.
 * @param volumeLabel ISO volume label: {@code CIDATA} for Linux cloud-init, {@code UNATTEND} for
 *     Windows sysprep.
 * @param osFamily {@code linux} or {@code windows} — used by providers to pick the right bus/slot.
 */
public record CustomizationSeed(String isoPath, String volumeLabel, String osFamily) {
  public boolean isLinux() {
    return "linux".equalsIgnoreCase(osFamily);
  }

  public boolean isWindows() {
    return "windows".equalsIgnoreCase(osFamily);
  }
}
