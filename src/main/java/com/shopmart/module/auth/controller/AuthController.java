package com.shopmart.module.auth.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.exception.TooManyRequestsException;
import com.shopmart.module.auth.dto.*;
import com.shopmart.module.auth.service.AuthService;
import com.shopmart.module.permission.service.PermissionService;
import com.shopmart.security.SecurityUtils;
import com.shopmart.security.ratelimit.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;
    private final RateLimiter rateLimiter;

    @Value("${app.security.rate-limit.login.max-attempts:5}")
    private int loginMaxAttempts;
    @Value("${app.security.rate-limit.login.window-seconds:900}")
    private int loginWindowSeconds;
    @Value("${app.security.rate-limit.otp.max-attempts:5}")
    private int otpMaxAttempts;
    @Value("${app.security.rate-limit.otp.window-seconds:900}")
    private int otpWindowSeconds;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registered. Please verify the OTP sent to your email.", user));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = clientIp(http);
        // Two independent buckets: per-IP (stops a single attacker hammering many accounts)
        // and per-email (stops distributed attempts at one account from many IPs).
        String ipKey = "login:ip:" + ip;
        String emailKey = "login:email:" + request.email().toLowerCase();
        if (!rateLimiter.tryConsume(ipKey, loginMaxAttempts, loginWindowSeconds)
                || !rateLimiter.tryConsume(emailKey, loginMaxAttempts, loginWindowSeconds)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later.");
        }
        AuthResponse response = authService.login(request, deviceInfo(http), clientIp(http));
        // Successful login clears both buckets so a legitimate user isn't penalized by earlier typos.
        rateLimiter.reset(ipKey);
        rateLimiter.reset(emailKey);
        return ApiResponse.ok("Login successful", response);
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in or sign up with a Google ID token (used by login & register pages)")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Login successful",
                authService.googleLogin(request.idToken(), deviceInfo(http), clientIp(http)));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Exchange a refresh token for a new token pair (rotates the old one)")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.refreshToken(request.refreshToken(), deviceInfo(http), clientIp(http)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out. Pass the refresh token to end just this session; " +
            "omit it to fall back to logging out every device.")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            authService.logoutSession(request.refreshToken());
        } else {
            authService.logoutAllDevices(SecurityUtils.currentUserId());
        }
        return ApiResponse.message("Logged out");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every session for the current user (log out of all devices)")
    public ApiResponse<Void> logoutAll() {
        authService.logoutAllDevices(SecurityUtils.currentUserId());
        return ApiResponse.message("Logged out of all devices");
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions/devices for the current user")
    public ApiResponse<List<SessionResponse>> sessions(@RequestHeader(value = "X-Refresh-Token", required = false) String currentRefreshToken) {
        return ApiResponse.ok(authService.listSessions(SecurityUtils.currentUserId(), currentRefreshToken));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Revoke a single session by id (sign a specific device out)")
    public ApiResponse<Void> revokeSession(@PathVariable Long id) {
        authService.revokeSession(SecurityUtils.currentUserId(), id);
        return ApiResponse.message("Session revoked");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the email-verification OTP")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody OtpRequest request) {
        String key = "otp:verify:" + request.email().toLowerCase();
        if (!rateLimiter.tryConsume(key, otpMaxAttempts, otpWindowSeconds)) {
            throw new TooManyRequestsException("Too many OTP attempts. Please try again later.");
        }
        authService.verifyOtp(request);
        rateLimiter.reset(key);
        return ApiResponse.message("Email verified");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend the email-verification OTP")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody EmailRequest request) {
        String key = "otp:resend:" + request.email().toLowerCase();
        if (!rateLimiter.tryConsume(key, otpMaxAttempts, otpWindowSeconds)) {
            throw new TooManyRequestsException("Too many OTP requests. Please try again later.");
        }
        authService.resendOtp(request.email());
        return ApiResponse.message("OTP resent");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a password-reset OTP")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        String key = "otp:forgot:" + request.email().toLowerCase();
        if (!rateLimiter.tryConsume(key, otpMaxAttempts, otpWindowSeconds)) {
            throw new TooManyRequestsException("Too many password reset requests. Please try again later.");
        }
        authService.forgotPassword(request.email());
        return ApiResponse.message("If that email exists, a reset code has been sent");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using an OTP")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String key = "otp:reset:" + request.email().toLowerCase();
        if (!rateLimiter.tryConsume(key, otpMaxAttempts, otpWindowSeconds)) {
            throw new TooManyRequestsException("Too many password reset attempts. Please try again later.");
        }
        authService.resetPassword(request);
        rateLimiter.reset(key);
        return ApiResponse.message("Password reset successful");
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public ApiResponse<UserResponse> getProfile() {
        return ApiResponse.ok(authService.getProfile(SecurityUtils.currentUserId()));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Get the current user's effective permissions " +
            "(base roles, permission codes, and unlocked screen keys)")
    public ApiResponse<com.shopmart.module.permission.dto.MyPermissionsResponse> myPermissions() {
        return ApiResponse.ok(permissionService.myPermissions(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's profile")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated",
                authService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), request);
        return ApiResponse.message("Password changed");
    }

    // ---- helpers ----

    private String deviceInfo(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        return ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua;
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
