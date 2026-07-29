package com.yorel.muxon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Binds {@code muxon.instanceName}, {@code muxon.instanceId}, and optional content-library paths.
 */
@ConfigurationProperties(prefix = "muxon", ignoreUnknownFields = true)
public class MuxonProperties {

    private String instanceName = "local";
    private int instanceId = 1;

    @NestedConfigurationProperty
    private ContentLibraries contentLibraries = new ContentLibraries();

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public ContentLibraries getContentLibraries() {
        return contentLibraries;
    }

    public void setContentLibraries(ContentLibraries contentLibraries) {
        this.contentLibraries = contentLibraries;
    }

    public static class ContentLibraries {

        /**
         * Root directory where uploaded binaries are stored using {@code providerRelativePath} layout.
         * For hypervisor access, mount this path (or a parent) on the storage pool. When empty, defaults to
         * {@code /var/lib/muxon/content-libraries}.
         */
        private String uploadArtifactRoot = "/var/lib/muxon/content-libraries";

        public String getUploadArtifactRoot() {
            return uploadArtifactRoot;
        }

        public void setUploadArtifactRoot(String uploadArtifactRoot) {
            this.uploadArtifactRoot = uploadArtifactRoot;
        }
    }
}
