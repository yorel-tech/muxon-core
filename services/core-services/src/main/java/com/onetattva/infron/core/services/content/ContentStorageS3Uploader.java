package com.onetattva.infron.core.services.content;

import com.onetattva.infron.db.model.ContentStorageEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Uploads content library artifacts to S3-compatible object storage using configuration from {@link ContentStorageEntity}.
 */
@Service
public class ContentStorageS3Uploader {

    public void uploadObject(ContentStorageEntity storage, Path file, String relativePath) {
        Map<String, Object> cfg = storage.getConfig() != null ? storage.getConfig() : Map.of();
        String bucket = requiredString(cfg, "bucket", "bucket");
        String key = buildObjectKey(cfg, relativePath);

        try (S3Client client = buildClient(cfg)) {
            PutObjectRequest put =
                    PutObjectRequest.builder().bucket(bucket).key(key).build();
            client.putObject(put, RequestBody.fromFile(file));
        }
    }

    /**
     * Hint URI for clients (not necessarily presigned).
     */
    public URI objectUri(ContentStorageEntity storage, String relativePath) {
        Map<String, Object> cfg = storage.getConfig() != null ? storage.getConfig() : Map.of();
        String bucket = requiredString(cfg, "bucket", "bucket");
        String key = buildObjectKey(cfg, relativePath);
        return URI.create("s3://" + bucket + "/" + key);
    }

    public boolean isS3(ContentStorageEntity storage) {
        return storage != null
                && storage.getStorageType() != null
                && "s3".equals(storage.getStorageType().toLowerCase(Locale.ROOT));
    }

    static String buildObjectKey(Map<String, Object> cfg, String relativePath) {
        String prefix = optionalString(cfg, "prefix");
        String rel = relativePath == null ? "" : relativePath.replace('\\', '/').replaceFirst("^/+", "");
        if (prefix == null || prefix.isBlank()) {
            return rel;
        }
        String p = prefix.replace('\\', '/').replaceAll("^/+|/+$", "");
        return p.isEmpty() ? rel : p + "/" + rel;
    }

    private static S3Client buildClient(Map<String, Object> cfg) {
        String regionStr = requiredString(cfg, "region", "region");
        Region region = Region.of(regionStr);
        String endpoint = optionalString(cfg, "endpoint");

        var builder = S3Client.builder().region(region);

        if (endpoint != null && !endpoint.isBlank()) {
            S3Configuration s3Configuration =
                    S3Configuration.builder().pathStyleAccessEnabled(true).build();
            builder.endpointOverride(URI.create(endpoint.trim())).serviceConfiguration(s3Configuration);
        }

        String accessKey = optionalString(cfg, "accessKeyId");
        String secret = optionalString(cfg, "secretAccessKey");
        if (accessKey != null
                && !accessKey.isBlank()
                && secret != null
                && !secret.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secret.trim())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    private static String requiredString(Map<String, Object> cfg, String key, String label) {
        String v = optionalString(cfg, key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("S3 content storage config missing " + label);
        }
        return v.trim();
    }

    private static String optionalString(Map<String, Object> cfg, String key) {
        Object o = cfg.get(key);
        return o != null ? o.toString() : null;
    }
}
