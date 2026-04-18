package com.krito.muxon.core.providers;

/**
 * Request to export a VM disk as a template file under a provider-relative destination path.
 */
public record VmTemplateExportRequest(
        String externalVmId,
        String destinationRelativePath,
        String templateName,
        ProviderContext providerContext,
        String correlationId
) {
}
