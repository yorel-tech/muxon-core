package com.krito.muxon.core.providers;

import java.util.Map;

public record VmTemplateExportResult(
        ResultType resultType,
        String templatePath,
        Long sizeBytes,
        Map<String, String> metadata,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILURE
    }

    public static VmTemplateExportResult success(String path, Long sizeBytes, Map<String, String> metadata) {
        return new VmTemplateExportResult(ResultType.SUCCESS, path, sizeBytes, metadata, null);
    }

    public static VmTemplateExportResult failure(ProviderError error) {
        return new VmTemplateExportResult(ResultType.FAILURE, null, null, null, error);
    }
}
