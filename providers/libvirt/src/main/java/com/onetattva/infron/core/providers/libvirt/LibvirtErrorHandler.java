package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.ProviderError;
import com.onetattva.infron.core.providers.ErrorCode;
import org.libvirt.LibvirtError;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static ErrorCode mapLibvirtError(LibvirtException e) {
        int errorCode = e.getError();

        return switch (errorCode) {
            case LibvirtError.VIR_ERR_NO_CONNECT -> ErrorCode.NETWORK_ERROR;
            case LibvirtError.VIR_ERR_OPERATION_DENIED -> ErrorCode.AUTHENTICATION_ERROR;
            case LibvirtError.VIR_ERR_INVALID_ARG -> ErrorCode.VALIDATION_ERROR;
            case LibvirtError.VIR_ERR_NO_DOMAIN -> ErrorCode.RESOURCE_UNAVAILABLE;
            case LibvirtError.VIR_ERR_SYSTEM_ERROR -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_OPERATION_INVALID -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_INTERNAL_ERROR -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_NO_MEMORY -> ErrorCode.RESOURCE_UNAVAILABLE;
            case LibvirtError.VIR_ERR_NO_SUPPORT -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_XML_ERROR -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_XML_DETAIL -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_UNKNOWN_HOST -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_CONFIG -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_MIGRATE_PERSIST -> ErrorCode.PROVIDER_ERROR;
            case LibvirtError.VIR_ERR_OPERATION_TIMEOUT -> ErrorCode.TIMEOUT;
            default -> {
                logger.warn("Unknown Libvirt error code: {}, mapping to PROVIDER_ERROR", errorCode);
                return ErrorCode.PROVIDER_ERROR;
            }
        };
    }

    /**
     * Determines if an error is retryable.
     *
     * @param e The Libvirt exception
     * @return true if error is retryable, false otherwise
     */
    public static boolean isRetryable(LibvirtException e) {
        int errorCode = e.getError();

        // Retry on network errors and temporary failures
        return errorCode == LibvirtError.VIR_ERR_NO_CONNECT ||
               errorCode == LibvirtError.VIR_ERR_SYSTEM_ERROR ||
               errorCode == LibvirtError.VIR_ERR_OPERATION_INVALID ||
               errorCode == LibvirtError.VIR_ERR_OPERATION_TIMEOUT ||
               errorCode == LibvirtError.VIR_ERR_INTERNAL_ERROR;
    }

    /**
     * Handles a Libvirt exception and returns a ProviderError.
     *
     * @param e The Libvirt exception
     * @param operation The operation being performed
     * @return ProviderError with mapped code and retry flag
     */
    public static ProviderError handleError(LibvirtException e, String operation) {
        ErrorCode code = mapLibvirtError(e);

        return ProviderError.builder()
                .code(code)
                .message(operation + " failed: " + e.getMessage())
                .providerErrorCode(String.valueOf(e.getError()))
                .details(Map.of(
                        "libvirtError", e.getError().toString(),
                        "libvirtMessage", e.getMessage()
                ))
                .retryable(isRetryable(e))
                .build();
    }
}
