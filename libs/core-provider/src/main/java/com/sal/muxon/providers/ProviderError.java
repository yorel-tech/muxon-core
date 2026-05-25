package com.sal.muxon.providers;

import java.util.Map;

/**
 * Provider error
 */
public record ProviderError(
        ErrorCode code,
        String message,
        String providerErrorCode,
        Map<String, Object> details,
        boolean retryable
) {
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
                    code,
                    message,
                    providerErrorCode,
                    details,
                    retryable != null ? retryable : false
            );
        }
    }
}
