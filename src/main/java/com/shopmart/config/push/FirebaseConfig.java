package com.shopmart.config.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Initializes the Firebase Admin SDK for real push delivery (Phase 3). Only active when
 * app.push.enabled=true — otherwise LoggingPushSender remains the active PushSender bean
 * and this config isn't even loaded, so a deployment without Firebase credentials configured
 * doesn't fail to start.
 *
 * Credentials come from FIREBASE_SERVICE_ACCOUNT_JSON (the full service-account JSON as a
 * single-line env var) or FIREBASE_SERVICE_ACCOUNT_BASE64 (the same, base64-encoded — often
 * easier to paste into Railway's env var UI without escaping issues). Generate this from the
 * Firebase console: Project Settings -> Service Accounts -> Generate new private key.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${app.push.fcm.service-account-json:}")
    private String serviceAccountJson;

    @Value("${app.push.fcm.service-account-base64:}")
    private String serviceAccountBase64;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        InputStream credentialsStream = resolveCredentials();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build();
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getApps().get(0);
        }
        log.info("[FCM] Firebase Admin SDK initialized");
        return FirebaseApp.initializeApp(options);
    }

    private InputStream resolveCredentials() {
        if (!serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        if (!serviceAccountBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64);
            return new ByteArrayInputStream(decoded);
        }
        throw new IllegalStateException(
                "app.push.enabled=true but neither FIREBASE_SERVICE_ACCOUNT_JSON nor " +
                "FIREBASE_SERVICE_ACCOUNT_BASE64 is set. Provide one, or set app.push.enabled=false " +
                "to fall back to the logging push sender.");
    }
}
