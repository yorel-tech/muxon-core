package com.krito.muxon.providers;

import java.util.List;

/**
 * Validation result
 */
public record ValidationResult(
        boolean valid,
        List<String> errors
) {
    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {
        private boolean valid;
        private List<String> errors;

        public ValidationResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public ValidationResultBuilder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(
                    valid,
                    errors != null ? errors : List.of()
            );
        }
    }
}
