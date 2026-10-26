package com.shopmart.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    @Value("${app.storage.s3.bucket}")
    private String bucket;
    @Value("${app.storage.s3.region}")
    private String region;
    @Value("${app.storage.s3.access-key}")
    private String accessKey;
    @Value("${app.storage.s3.secret-key}")
    private String secretKey;
    @Value("${app.storage.s3.public-base-url:}")
    private String publicBaseUrl;

    private S3Client client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Override
    public UploadResult upload(MultipartFile file, String folder) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + (ext != null ? "." + ext : "");
        try (S3Client s3 = client()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }
        String base = publicBaseUrl.isBlank()
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicBaseUrl;
        return new UploadResult(base + "/" + key, key);
    }

    @Override
    public void delete(String storageKey) {
        try (S3Client s3 = client()) {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        } catch (Exception e) {
            log.warn("[STORAGE:s3] failed to delete {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public String provider() {
        return "s3";
    }
}
