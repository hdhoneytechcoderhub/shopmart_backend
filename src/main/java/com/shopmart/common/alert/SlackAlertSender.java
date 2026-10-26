package com.shopmart.common.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Active when app.alerts.channel=slack. Posts to a Slack incoming webhook URL
 *  (Slack -> Apps -> Incoming Webhooks -> Add to workspace). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.alerts.channel", havingValue = "slack")
public class SlackAlertSender implements AlertSender {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;

    @Value("${app.alerts.slack.webhook-url:}")
    private String webhookUrl;

    @Override
    public void send(String subject, String message, AlertSeverity severity) {
        if (webhookUrl.isBlank()) {
            log.warn("[ALERT:slack] app.alerts.channel=slack but no webhook-url configured — dropping alert: {}", subject);
            return;
        }
        String emoji = switch (severity) {
            case CRITICAL -> ":rotating_light:";
            case WARNING -> ":warning:";
            case INFO -> ":information_source:";
        };
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "text", emoji + " *" + severity + "* " + subject + "\n" + message));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("[ALERT:slack] webhook returned status={}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("[ALERT:slack] failed to send alert '{}': {}", subject, e.getMessage());
        }
    }
}
