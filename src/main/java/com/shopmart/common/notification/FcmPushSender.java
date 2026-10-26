package com.shopmart.common.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.shopmart.module.push.entity.DeviceToken;
import com.shopmart.module.push.entity.PushDeliveryLog;
import com.shopmart.module.push.repository.PushDeviceTokenRepository;
import com.shopmart.module.push.repository.PushDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Real FCM delivery (Phase 3). Active only when app.push.enabled=true (see FirebaseConfig).
 * Sends to every active device token registered for the user, via a single multicast call.
 * Tokens FCM reports as unregistered/invalid are deactivated so future sends don't keep
 * retrying a dead token — this is the "Push Failure Handling" requirement, not just a try/catch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    private final FirebaseApp firebaseApp;
    private final PushDeviceTokenRepository deviceTokenRepository;
    private final PushDeliveryLogRepository deliveryLogRepository;

    @Override
    @Transactional
    public void send(Long userId, String title, String message, String link) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        PushDeliveryLog logEntry = new PushDeliveryLog();
        logEntry.setUserId(userId);
        logEntry.setTitle(title);
        logEntry.setAttemptedTokens(tokens.size());

        if (tokens.isEmpty()) {
            logEntry.setStatus(PushDeliveryLog.DeliveryStatus.NO_DEVICES);
            deliveryLogRepository.save(logEntry);
            log.info("[FCM] userId={} has no registered devices - skipping", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream().map(DeviceToken::getToken).toList();
        Notification notification = Notification.builder().setTitle(title).setBody(message).build();
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(tokenStrings);
        if (link != null && !link.isBlank()) {
            builder.putData("link", link);
        }
        MulticastMessage multicastMessage = builder.build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp)
                    .sendEachForMulticast(multicastMessage);

            List<String> deadTokens = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (!r.isSuccessful() && r.getException() != null) {
                    MessagingErrorCode code = r.getException().getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        deadTokens.add(tokenStrings.get(i));
                    } else {
                        log.warn("[FCM] delivery failed userId={} token={} error={}",
                                userId, mask(tokenStrings.get(i)), code);
                    }
                }
            }
            if (!deadTokens.isEmpty()) {
                deadTokens.forEach(deviceTokenRepository::deleteByToken);
                log.info("[FCM] removed {} dead token(s) for userId={}", deadTokens.size(), userId);
            }

            logEntry.setDeliveredTokens(response.getSuccessCount());
            logEntry.setStatus(response.getFailureCount() == 0
                    ? PushDeliveryLog.DeliveryStatus.SENT
                    : (response.getSuccessCount() > 0 ? PushDeliveryLog.DeliveryStatus.PARTIAL : PushDeliveryLog.DeliveryStatus.FAILED));
            log.info("[FCM] userId={} sent={} failed={}", userId, response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            logEntry.setStatus(PushDeliveryLog.DeliveryStatus.FAILED);
            logEntry.setFailureReason(e.getMessage());
            log.error("[FCM] send failed userId={}: {}", userId, e.getMessage());
        } finally {
            deliveryLogRepository.save(logEntry);
        }
    }

    private String mask(String token) {
        return token.length() > 8 ? token.substring(0, 8) + "..." : "***";
    }
}
