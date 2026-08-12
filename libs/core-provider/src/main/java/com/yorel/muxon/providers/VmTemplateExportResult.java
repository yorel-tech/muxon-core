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

import java.util.Map;

public record VmTemplateExportResult(
    ResultType resultType,
    String templatePath,
    Long sizeBytes,
    Map<String, String> metadata,
    ProviderError error) {
  public enum ResultType {
    SUCCESS,
    FAILURE
  }

  public static VmTemplateExportResult success(
      String path, Long sizeBytes, Map<String, String> metadata) {
    return new VmTemplateExportResult(ResultType.SUCCESS, path, sizeBytes, metadata, null);
  }

  public static VmTemplateExportResult failure(ProviderError error) {
    return new VmTemplateExportResult(ResultType.FAILURE, null, null, null, error);
  }
}
