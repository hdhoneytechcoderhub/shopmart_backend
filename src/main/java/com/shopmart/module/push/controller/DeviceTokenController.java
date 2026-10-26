package com.shopmart.module.push.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.push.dto.DeviceRegisterRequest;
import com.shopmart.module.push.dto.DeviceTokenResponse;
import com.shopmart.module.push.entity.DeviceToken;
import com.shopmart.module.push.repository.PushDeviceTokenRepository;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Phase 3: "Device Registration APIs" / "Device Removal APIs". Any authenticated user can
 * register/remove their own device tokens — there's no admin surface for this because a device
 * token is only meaningful in the context of the user who owns the device.
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Push Devices")
public class DeviceTokenController {

    private final PushDeviceTokenRepository repository;

    @PostMapping("/register")
    @Transactional
    public ApiResponse<DeviceTokenResponse> register(@Valid @RequestBody DeviceRegisterRequest request) {
        Long userId = SecurityUtils.currentUserId();
        // Re-registering an existing token (app reopened, token unchanged) updates ownership/
        // last-seen rather than creating a duplicate row — tokens are globally unique per device.
        DeviceToken token = repository.findByToken(request.token()).orElseGet(DeviceToken::new);
        token.setUserId(userId);
        token.setToken(request.token());
        token.setPlatform(request.platform());
        token.setActive(true);
        token.setLastSeenAt(Instant.now());
        token = repository.save(token);
        return ApiResponse.ok("Device registered",
                new DeviceTokenResponse(token.getId(), token.getPlatform(), token.getLastSeenAt(), token.getCreatedAt()));
    }

    @DeleteMapping("/{token}")
    @Transactional
    public ApiResponse<Void> unregister(@PathVariable String token) {
        repository.findByToken(token).ifPresent(t -> {
            if (t.getUserId().equals(SecurityUtils.currentUserId())) {
                repository.delete(t);
            }
        });
        return ApiResponse.message("Device removed");
    }

    @GetMapping
    public ApiResponse<List<DeviceTokenResponse>> myDevices() {
        Long userId = SecurityUtils.currentUserId();
        List<DeviceTokenResponse> devices = repository.findByUserIdAndActiveTrue(userId).stream()
                .map(t -> new DeviceTokenResponse(t.getId(), t.getPlatform(), t.getLastSeenAt(), t.getCreatedAt()))
                .toList();
        return ApiResponse.ok(devices);
    }
}
