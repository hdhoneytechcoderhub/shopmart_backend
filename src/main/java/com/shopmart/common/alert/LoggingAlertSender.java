package com.shopmart.common.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default alert channel: logs at ERROR/WARN so it's visible in Railway's log stream and
 *  picked up by any log-based alerting you already have, even with zero extra config. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.alerts.channel", havingValue = "log", matchIfMissing = true)
public class LoggingAlertSender implements AlertSender {
    @Override
    public void send(String subject, String message, AlertSeverity severity) {
        switch (severity) {
            case CRITICAL -> log.error("[ALERT:CRITICAL] {} - {}", subject, message);
            case WARNING -> log.warn("[ALERT:WARNING] {} - {}", subject, message);
            default -> log.info("[ALERT:INFO] {} - {}", subject, message);
        }
    }
}
