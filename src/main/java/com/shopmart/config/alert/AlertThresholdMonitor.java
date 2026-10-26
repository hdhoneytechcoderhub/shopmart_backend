package com.shopmart.config.alert;

import com.shopmart.common.alert.AlertSender;
import com.shopmart.common.alert.AlertSeverity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls the business counters registered in MetricsConfig and fires an alert when the delta
 * since the last poll exceeds a configured threshold.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertThresholdMonitor {

    private final MeterRegistry meterRegistry;
    private final AlertSender alertSender;

    @Value("${app.alerts.thresholds.errors-per-window:10}")
    private double errorThreshold;

    @Value("${app.alerts.thresholds.payment-failures-per-window:5}")
    private double paymentFailureThreshold;

    @Value("${app.alerts.thresholds.webhook-rejections-per-window:5}")
    private double webhookRejectionThreshold;

    @Value("${app.alerts.thresholds.notification-failures-per-window:10}")
    private double notificationFailureThreshold;

    @Value("${app.alerts.thresholds.rate-limit-blocks-per-window:20}")
    private double rateLimitThreshold;

    private final ConcurrentHashMap<String, Double> lastTotals = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${app.alerts.poll-interval-ms:300000}")
    @SchedulerLock(
            name = "alertThresholdMonitor",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    public void checkThresholds() {

        checkCounterFamily(
                "shopmart.errors.unhandled",
                errorThreshold,
                "High error rate",
                "unhandled exceptions in the last check window"
        );

        checkTaggedCounter(
                "shopmart.payments.result",
                "result",
                "failed",
                paymentFailureThreshold,
                "Payment failures",
                "payment verification failures in the last check window"
        );

        checkTaggedCounter(
                "shopmart.webhooks.result",
                "result",
                "rejected",
                webhookRejectionThreshold,
                "Webhook rejections",
                "rejected webhook signatures in the last check window — verify webhook secrets are configured correctly, or this may indicate spoofed requests"
        );

        checkCounterFamily(
                "shopmart.notifications.failed",
                notificationFailureThreshold,
                "Notification delivery failures",
                "failed notification/campaign deliveries in the last check window"
        );

        checkCounterFamily(
                "shopmart.ratelimit.blocked",
                rateLimitThreshold,
                "Elevated rate-limit blocks",
                "rate-limited requests in the last check window — could be a brute-force attempt"
        );
    }

    private void checkCounterFamily(
            String name,
            double threshold,
            String subject,
            String descriptionSuffix
    ) {
        double total = meterRegistry.find(name)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();

        evaluate(name, total, threshold, subject, descriptionSuffix);
    }

    private void checkTaggedCounter(
            String name,
            String tagKey,
            String tagValue,
            double threshold,
            String subject,
            String descriptionSuffix
    ) {
        double total = meterRegistry.find(name)
                .tag(tagKey, tagValue)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();

        evaluate(name + ":" + tagValue, total, threshold, subject, descriptionSuffix);
    }

    private void evaluate(
            String key,
            double total,
            double threshold,
            String subject,
            String descriptionSuffix
    ) {
        double previous = lastTotals.getOrDefault(key, total);
        double delta = total - previous;

        lastTotals.put(key, total);

        if (delta >= threshold) {
            alertSender.send(
                    subject,
                    (long) delta + " " + descriptionSuffix,
                    AlertSeverity.WARNING
            );
        }
    }
}