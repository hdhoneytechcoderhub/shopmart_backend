package com.shopmart.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Default provider (app.storage.provider=local, or unset). Writes to local disk — fine for
 * local development, WRONG for Railway production (ephemeral filesystem: files vanish on
 * every redeploy/restart). Set app.storage.provider=s3 or cloudinary before shipping any real
 * upload feature to production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalDiskFileStorageService implements FileStorageService {

    @Value("${app.storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${app.storage.local.public-base-url:http://localhost:8080/uploads}")
    private String publicBaseUrl;

    @Override
    public UploadResult upload(MultipartFile file, String folder) {
        try {
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String key = folder + "/" + UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path target = Path.of(basePath, key);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            String url = publicBaseUrl + "/" + key;
            log.warn("[STORAGE:local] wrote {} to local disk — this does NOT survive a Railway " +
                    "redeploy; switch app.storage.provider to s3/cloudinary before production use", target);
            return new UploadResult(url, key);
        } catch (IOException e) {
            throw new RuntimeException("Local file upload failed", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(Path.of(basePath, storageKey));
        } catch (IOException e) {
            log.warn("[STORAGE:local] failed to delete {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public String provider() {
        return "local";
    }
}
