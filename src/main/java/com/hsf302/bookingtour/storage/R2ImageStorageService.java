package com.hsf302.bookingtour.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
public class R2ImageStorageService {

    private final boolean enabled;
    private final String region;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String publicBaseUrl;
    private final long maxImageSizeBytes;

    public R2ImageStorageService(@Value("${tour.s3.enabled}") boolean enabled,
                                 @Value("${tour.s3.region}") String region,
                                 @Value("${tour.s3.bucket}") String bucket,
                                 @Value("${tour.s3.access-key}") String accessKey,
                                 @Value("${tour.s3.secret-key}") String secretKey,
                                 @Value("${tour.s3.endpoint}") String endpoint,
                                 @Value("${tour.s3.public-base-url}") String publicBaseUrl,
                                 @Value("${tour.s3.max-image-size-bytes}") long maxImageSizeBytes) {
        this.enabled = enabled;
        this.region = region;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        this.publicBaseUrl = publicBaseUrl;
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public String uploadTourImage(MultipartFile file) {
        validateConfig();
        validateImage(file);

        String objectKey = "tours/" + UUID.randomUUID() + extensionFrom(file.getOriginalFilename(), file.getContentType());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.getContentType())
                .cacheControl("public, max-age=31536000")
                .build();

        try (S3Client client = createClient()) {
            client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read uploaded image.", exception);
        }

        return publicBaseUrl.replaceAll("/+$", "") + "/" + objectKey;
    }

    private S3Client createClient() {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private void validateConfig() {
        if (!enabled) {
            throw new IllegalStateException("R2 image upload is disabled.");
        }
        if (isBlank(bucket) || isBlank(accessKey) || isBlank(secretKey) || isBlank(endpoint) || isBlank(publicBaseUrl)) {
            throw new IllegalStateException("R2 image upload is missing required configuration.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image file.");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new IllegalArgumentException("Image is too large. Maximum size is " + maxImageSizeBytes + " bytes.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }
    }

    private String extensionFrom(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
