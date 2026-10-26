package com.shopmart.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Phase 5: file storage abstraction. This backend was audited before writing any of this —
 * there is currently NO server-side file upload endpoint anywhere in the codebase (no
 * MultipartFile handling, no local disk writes). Product images / compliance documents are
 * stored purely as URL strings, which means uploads currently happen outside this backend
 * entirely (frontend direct-to-provider, or an admin process not present in this codebase
 * snapshot) — there is nothing on Railway's local disk to migrate away from in THIS backend.
 *
 * This abstraction is built anyway, ready to wire up, because the moment a real upload
 * endpoint is needed (this session adds one example: product image upload), it should not
 * write to ephemeral local disk. Swap providers via app.storage.provider — local disk remains
 * the zero-config default for dev, S3/Cloudinary are real implementations for production.
 */
public interface FileStorageService {
    /** Uploads and returns both the public URL and a provider-specific key that delete() needs. */
    UploadResult upload(MultipartFile file, String folder);

    /** Deletes by the storageKey returned in UploadResult at upload time — not by URL, since
     *  S3/Cloudinary keys and public URLs aren't always the same string (e.g. CDN domains). */
    void delete(String storageKey);

    String provider();

    record UploadResult(String url, String storageKey) {}
}
