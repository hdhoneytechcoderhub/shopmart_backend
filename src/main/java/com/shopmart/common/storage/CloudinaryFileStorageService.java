package com.shopmart.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(@Value("${app.storage.cloudinary.cloud-name}") String cloudName,
                                        @Value("${app.storage.cloudinary.api-key}") String apiKey,
                                        @Value("${app.storage.cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public UploadResult upload(MultipartFile file, String folder) {
        try {
            String publicId = folder + "/" + UUID.randomUUID();
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("public_id", publicId, "folder", folder, "resource_type", "auto"));
            String url = (String) result.get("secure_url");
            String storageKey = (String) result.get("public_id");
            return new UploadResult(url, storageKey);
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            cloudinary.uploader().destroy(storageKey, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("[STORAGE:cloudinary] failed to delete {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public String provider() {
        return "cloudinary";
    }
}
