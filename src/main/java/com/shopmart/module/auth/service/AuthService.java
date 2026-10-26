package com.shopmart.module.auth.service;

import com.shopmart.module.auth.dto.*;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress);
    AuthResponse googleLogin(String idToken, String deviceInfo, String ipAddress);
    AuthResponse refreshToken(String refreshToken, String deviceInfo, String ipAddress);
    /** Revoke a single session (the device that presented this refresh token). */
    void logoutSession(String refreshToken);
    /** Revoke every session for this user (logout of all devices). */
    void logoutAllDevices(Long userId);
    java.util.List<SessionResponse> listSessions(Long userId, String currentRefreshToken);
    void revokeSession(Long userId, Long sessionId);
    void verifyOtp(OtpRequest request);
    void resendOtp(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    UserResponse getProfile(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
