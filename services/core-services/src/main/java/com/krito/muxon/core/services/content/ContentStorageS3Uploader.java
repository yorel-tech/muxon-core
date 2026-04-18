package com.krito.muxon.core.services.content;

import com.krito.muxon.db.model.ContentStorageEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
     * Best-effort delete of one object (S3 succeeds even when the key does not exist).
     */
    public void deleteRelativeObject(ContentStorageEntity storage, String relativePath) {
        if (!isS3(storage)) {
            throw new IllegalArgumentException("Not S3 storage");
        }
        Map<String, Object> cfg = storage.getConfig() != null ? storage.getConfig() : Map.of();
        String bucket = requiredString(cfg, "bucket", "bucket");
        String rel = relativePath == null ? "" : relativePath.replace('\\', '/').replaceFirst("^/+", "");
        String key = buildObjectKey(cfg, rel);
        try (S3Client client = buildClient(cfg)) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
    }

    /**
     * Deletes every object whose key starts with the prefix derived from {@code relativeDirectoryPrefix}
     * (same rules as {@link #buildObjectKey(Map, String)}). The prefix is treated as a directory: a trailing
     * {@code /} is appended after applying storage config so only keys under that path are removed.
     */
    public void deleteAllUnderRelativePrefix(ContentStorageEntity storage, String relativeDirectoryPrefix) {
        if (!isS3(storage)) {
            throw new IllegalArgumentException("Not S3 storage");
        }
        Map<String, Object> cfg = storage.getConfig() != null ? storage.getConfig() : Map.of();
        String bucket = requiredString(cfg, "bucket", "bucket");
        String normalized =
                relativeDirectoryPrefix == null ? "" : relativeDirectoryPrefix.replace('\\', '/').replaceFirst("^/+", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("relativeDirectoryPrefix must not be empty");
        }
        String keyPrefix = buildObjectKey(cfg, normalized);
        if (!keyPrefix.endsWith("/")) {
            keyPrefix = keyPrefix + "/";
        }

        try (S3Client client = buildClient(cfg)) {
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder listReq =
                        ListObjectsV2Request.builder().bucket(bucket).prefix(keyPrefix);
                if (continuationToken != null) {
                    listReq.continuationToken(continuationToken);
                }
                var response = client.listObjectsV2(listReq.build());
                List<S3Object> contents = response.contents();
                if (!contents.isEmpty()) {
                    List<ObjectIdentifier> toDelete = new ArrayList<>(contents.size());
                    for (S3Object obj : contents) {
                        toDelete.add(ObjectIdentifier.builder().key(obj.key()).build());
                    }
                    for (int i = 0; i < toDelete.size(); i += 1000) {
                        int end = Math.min(i + 1000, toDelete.size());
                        DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
                                .bucket(bucket)
                                .delete(Delete.builder()
                                        .objects(toDelete.subList(i, end))
                                        .build())
                                .build();
                        client.deleteObjects(delReq);
                    }
                }
                continuationToken = Boolean.TRUE.equals(response.isTruncated())
                        ? response.nextContinuationToken()
                        : null;
            } while (continuationToken != null);
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
