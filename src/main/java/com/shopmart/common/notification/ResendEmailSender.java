package com.shopmart.common.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
public class ResendEmailSender implements EmailSender {

    private static final URI SEND_EMAIL_URI = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String from;

    public ResendEmailSender(ObjectMapper objectMapper, String apiKey, String from) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) return;
        if (apiKey.isBlank() || from.isBlank()) {
            log.error("Resend email delivery is not configured; set RESEND_API_KEY and RESEND_FROM_EMAIL");
            throw new NotificationDeliveryException();
        }

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "text", body
            ));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize Resend email request");
            throw new NotificationDeliveryException(e);
        }

        HttpRequest request = HttpRequest.newBuilder(SEND_EMAIL_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Resend rejected email delivery with HTTP status {}", response.statusCode());
                throw new NotificationDeliveryException();
            }
            log.info("Resend accepted email delivery with HTTP status {}", response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Resend email delivery was interrupted");
            throw new NotificationDeliveryException(e);
        } catch (IOException e) {
            log.error("Resend email delivery request failed");
            throw new NotificationDeliveryException(e);
        }
    }
}
