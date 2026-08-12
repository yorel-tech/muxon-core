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
package com.yorel.muxon.providers.libvirt;

import com.yorel.muxon.providers.ProviderError;
import java.util.Map;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps Libvirt errors to provider errors.
 *
 * <p>Provides consistent error handling across different Libvirt error codes and determines
 * retryability of failures.
 */
public class LibvirtErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(LibvirtErrorHandler.class);

  /**
   * Maps a Libvirt exception to a provider error code.
   *
   * @param e The Libvirt exception
   * @return Mapped ErrorCode
   */
  public static ProviderError.ErrorCode mapLibvirtError(LibvirtException e) {
    try {
      String errorMessage = e.getMessage();

      // Map based on error message content
      if (errorMessage != null) {
        if (errorMessage.contains("connection") || errorMessage.contains("network")) {
          return ProviderError.ErrorCode.NETWORK_ERROR;
        } else if (errorMessage.contains("auth") || errorMessage.contains("permission")) {
          return ProviderError.ErrorCode.AUTHENTICATION_ERROR;
        } else if (errorMessage.contains("invalid") || errorMessage.contains("validation")) {
          return ProviderError.ErrorCode.VALIDATION_ERROR;
        } else if (errorMessage.contains("resource") || errorMessage.contains("no space")) {
          return ProviderError.ErrorCode.RESOURCE_UNAVAILABLE;
        } else if (errorMessage.contains("timeout")) {
          return ProviderError.ErrorCode.TIMEOUT;
        }
      }

      return ProviderError.ErrorCode.PROVIDER_ERROR;
    } catch (Exception ex) {
      logger.error("Error mapping Libvirt exception: {}", ex.getMessage());
      return ProviderError.ErrorCode.PROVIDER_ERROR;
    }
  }

  /**
   * Determines if an error is retryable.
   *
   * @param e The Libvirt exception
   * @return true if error is retryable, false otherwise
   */
  public static boolean isRetryable(LibvirtException e) {
    try {
      String errorMessage = e.getMessage();

      // Consider network errors and timeouts as retryable
      if (errorMessage != null) {
        return errorMessage.contains("connection")
            || errorMessage.contains("timeout")
            || errorMessage.contains("network");
      }

      return false;
    } catch (Exception ex) {
      logger.error("Error checking retryability of Libvirt exception: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Handles a Libvirt exception and returns a ProviderError.
   *
   * @param e The Libvirt exception
   * @param operation The operation being performed
   * @return ProviderError with mapped code and retry flag
   */
  public static ProviderError handleError(LibvirtException e, String operation) {
    ProviderError.ErrorCode code = mapLibvirtError(e);

    return ProviderError.builder()
        .code(code)
        .message(operation + " failed: " + e.getMessage())
        .providerErrorCode("LIBVIRT_ERROR")
        .details(Map.of("libvirtMessage", e.getMessage()))
        .retryable(isRetryable(e))
        .build();
  }
}
