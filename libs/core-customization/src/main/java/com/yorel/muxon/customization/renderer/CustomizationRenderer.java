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
package com.yorel.muxon.customization.renderer;

import com.yorel.muxon.customization.model.VmCustomizationSpec;
import java.util.Map;

/** Renders a {@link VmCustomizationSpec} into a set of named files to be placed on the seed ISO. */
public interface CustomizationRenderer {

  /**
   * Render all files that should be placed on the seed ISO.
   *
   * @param vmId VM UUID string (used as the cloud-init instance-id).
   * @param vmName VM name (used as the fallback hostname when spec.hostname is blank).
   * @param spec Fully merged and secret-decrypted customization spec.
   * @return Map of filename → file content (UTF-8 string).
   */
  Map<String, String> render(String vmId, String vmName, VmCustomizationSpec spec);

  /** Volume label to apply to the seed ISO. */
  String volumeLabel();
}
