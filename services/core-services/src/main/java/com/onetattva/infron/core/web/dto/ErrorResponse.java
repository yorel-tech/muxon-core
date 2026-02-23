package com.onetattva.infron.core.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard error response structure for API errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String code;
    private String message;
    private Long timestamp;
    private Map<String, String> details;
    private String path;
    
    public ErrorResponse() {
    }
    
    public ErrorResponse(String code, String message, Long timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public ErrorResponse(String code, String message, Long timestamp, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.details = details;
    }
    
    public ErrorResponse(String code, String message, Long timestamp, Map<String, String> details, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.details = details;
        this.path = path;
    }
    
    // Getters and setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String code;
        private String message;
        private Long timestamp;
        private Map<String, String> details;
        private String path;
        
        public Builder code(String code) {
            this.code = code;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder details(Map<String, String> details) {
            this.details = details;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(code, message, timestamp, details, path);
        }
    }
}
