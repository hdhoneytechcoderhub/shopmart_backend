package com.shopmart.common.storage.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.storage.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic admin upload endpoint, backed by whichever FileStorageService is active
 * (app.storage.provider). Not tied to a specific entity (product/compliance/etc.) — the
 * caller uploads a file, gets back a URL, and stores that URL wherever it's needed (e.g. a
 * product's image field), exactly as the existing URL-string-based entities already expect.
 * This is intentionally the smallest change that makes the storage abstraction from Phase 5
 * actually usable, without restructuring entities that don't currently have upload endpoints.
 */
@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "File Storage")
public class FileUploadController {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final java.util.Set<String> ALLOWED_FOLDERS = java.util.Set.of(
            "products", "compliance", "banners", "brands", "misc");

    private final FileStorageService storageService;

    @PostMapping("/upload")
    public ApiResponse<FileStorageService.UploadResult> upload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "misc") String folder) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        if (file.getSize() > MAX_SIZE_BYTES) throw new BadRequestException("File exceeds 10MB limit");
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new BadRequestException("Unknown folder '" + folder + "', expected one of " + ALLOWED_FOLDERS);
        }
        return ApiResponse.ok("Uploaded via " + storageService.provider(), storageService.upload(file, folder));
    }

    public record DeleteRequest(String storageKey) {}

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody DeleteRequest request) {
        if (request.storageKey() == null || request.storageKey().isBlank()) {
            throw new BadRequestException("storageKey is required");
        }
        storageService.delete(request.storageKey());
        return ApiResponse.message("Deleted");
    }
}
