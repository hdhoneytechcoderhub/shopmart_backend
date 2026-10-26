package com.shopmart.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default push channel: logs instead of sending. Active unless app.push.enabled=true.
 * Mirrors LoggingEmailSender/LoggingSmsSender. Wiring a real provider (FCM/APNs) means adding
 * a device-token store (not present in this codebase) and an HttpPushSender analogous to
 * HttpSmsSender — that's genuinely new infrastructure, not something to fake here.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.push.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPushSender implements PushSender {
    @Override
    public void send(Long userId, String title, String message, String link) {
        log.info("[PUSH:log] userId={} title={} message={} link={}", userId, title, message, link);
    }
}
