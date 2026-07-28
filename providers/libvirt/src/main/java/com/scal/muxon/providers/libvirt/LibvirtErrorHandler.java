package com.scal.muxon.providers.libvirt;

import com.scal.muxon.providers.ProviderError;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Libvirt errors to provider errors.
 *
 * <p>Provides consistent error handling across different Libvirt error codes
 * and determines retryability of failures.</p>
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
                return errorMessage.contains("connection") ||
                       errorMessage.contains("timeout") ||
                       errorMessage.contains("network");
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
                .details(Map.of(
                        "libvirtMessage", e.getMessage()
                ))
                .retryable(isRetryable(e))
                .build();
    }
}
