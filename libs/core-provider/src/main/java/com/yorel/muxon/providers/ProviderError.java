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

/** Provider error */
public record ProviderError(
    ErrorCode code,
    String message,
    String providerErrorCode,
    Map<String, Object> details,
    boolean retryable) {
  public enum ErrorCode {
    VALIDATION_ERROR,
    RESOURCE_UNAVAILABLE,
    QUOTA_EXCEEDED,
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    PROVIDER_ERROR,
    TIMEOUT,
    UNKNOWN_ERROR
  }

  public static ProviderErrorBuilder builder() {
    return new ProviderErrorBuilder();
  }

  public static class ProviderErrorBuilder {
    private ErrorCode code;
    private String message;
    private String providerErrorCode;
    private Map<String, Object> details;
    private Boolean retryable;

    public ProviderErrorBuilder code(ErrorCode code) {
      this.code = code;
      return this;
    }

    public ProviderErrorBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ProviderErrorBuilder providerErrorCode(String providerErrorCode) {
      this.providerErrorCode = providerErrorCode;
      return this;
    }

    public ProviderErrorBuilder details(Map<String, Object> details) {
      this.details = details;
      return this;
    }

    public ProviderErrorBuilder retryable(boolean retryable) {
      this.retryable = retryable;
      return this;
    }

    public ProviderError build() {
      return new ProviderError(
          code, message, providerErrorCode, details, retryable != null ? retryable : false);
    }
  }
}
