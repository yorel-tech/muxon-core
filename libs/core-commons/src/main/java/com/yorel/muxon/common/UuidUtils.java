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
package com.yorel.muxon.common;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import java.util.UUID;

public class UuidUtils {
  /**
   * Validates if the given string is a valid UUID.
   *
   * @param uuidString The string to check.
   * @return true if the string is a valid UUID, false otherwise.
   */
  public static boolean isValidUUID(String uuidString) {
    if (uuidString == null) {
      return false;
    }
    try {
      UUID.fromString(uuidString);
      return true;
    } catch (IllegalArgumentException exception) {
      // The string is not a valid UUID format
      return false;
    }
  }

  /**
   * Generates a UUID v5 based on the given namespace and name.
   *
   * @param namespace The namespace UUID.
   * @param name The name string.
   * @return The generated UUID v5.
   */
  public static UUID generateUuid5(UUID namespace, String name) {
    NameBasedGenerator gen = Generators.nameBasedGenerator(namespace);
    return gen.generate(name);
  }
}
