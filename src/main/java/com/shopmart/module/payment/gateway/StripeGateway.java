package com.shopmart.module.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.util.HmacUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Stripe PaymentIntents API. Active when app.payments.provider=stripe.
 * createGatewayOrder -> creates a PaymentIntent, returns its id (client uses the client_secret).
 * verifySignature   -> retrieves the PaymentIntent and confirms status=succeeded (signature param unused).
 * Uses the JDK HTTP client (no SDK dependency).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "stripe")
public class StripeGateway implements PaymentGateway {

    private static final String INTENTS_URL = "https://api.stripe.com/v1/payment_intents";

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final String secretKey;
    private final String webhookSecret;

    public StripeGateway(ObjectMapper objectMapper,
                         @Value("${app.payments.stripe.secret-key:}") String secretKey,
                         @Value("${app.payments.stripe.webhook-secret:}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String provider() {
        return "stripe";
    }

    @Override
    public String createGatewayOrder(Long orderId, BigDecimal amount, String currency) {
        if (secretKey.isBlank()) {
            throw new BadRequestException("Stripe secret key is not configured");
        }
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String form = "amount=" + minor
                + "&currency=" + URLEncoder.encode(currency.toLowerCase(), StandardCharsets.UTF_8)
                + "&automatic_payment_methods[enabled]=true"
                + "&metadata[order_id]=" + orderId;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(INTENTS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][stripe] intent create failed status={} body={}", resp.statusCode(), resp.body());
                throw new BadRequestException("Stripe payment intent creation failed");
            }
            JsonNode node = objectMapper.readTree(resp.body());
            return node.path("id").asText();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PAYMENT][stripe] intent create error: {}", e.getMessage());
            throw new BadRequestException("Stripe payment intent creation error");
        }
    }

    @Override
    public boolean verifySignature(String gatewayRef, String transactionId, String signature) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(INTENTS_URL + "/" + gatewayRef))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][stripe] intent fetch failed status={}", resp.statusCode());
                return false;
            }
            String status = objectMapper.readTree(resp.body()).path("status").asText();
            boolean ok = "succeeded".equals(status);
            log.info("[PAYMENT][stripe] verify intent={} status={} -> {}", gatewayRef, status, ok);
            return ok;
        } catch (Exception e) {
            log.error("[PAYMENT][stripe] verify error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // Stripe-Signature header format: "t=<timestamp>,v1=<hex hmac>[,v0=...]"
        // Expected v1 = HMAC_SHA256(timestamp + "." + payload, webhook signing secret).
        if (webhookSecret.isBlank()) {
            log.error("[PAYMENT][stripe] webhook secret not configured - rejecting webhook (fail-closed)");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[PAYMENT][stripe] webhook missing Stripe-Signature header - rejecting");
            return false;
        }
        String timestamp = null;
        String v1 = null;
        for (String part : signature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if ("t".equals(kv[0])) timestamp = kv[1];
            else if ("v1".equals(kv[0])) v1 = kv[1];
        }
        if (timestamp == null || v1 == null) {
            log.warn("[PAYMENT][stripe] malformed Stripe-Signature header - rejecting");
            return false;
        }
        String expected = HmacUtil.hmacSha256Hex(timestamp + "." + payload, webhookSecret);
        boolean ok = HmacUtil.constantTimeEquals(expected, v1);
        log.info("[PAYMENT][stripe] webhook signature verify -> {}", ok);
        return ok;
    }

    @Override
    public GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(INTENTS_URL + "/" + gatewayRef))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return GatewayStatus.UNKNOWN;
            String status = objectMapper.readTree(resp.body()).path("status").asText("");
            return switch (status) {
                case "succeeded" -> GatewayStatus.SUCCESS;
                case "canceled" -> GatewayStatus.FAILED;
                case "requires_payment_method", "requires_confirmation", "requires_action", "processing" -> GatewayStatus.PENDING;
                default -> GatewayStatus.UNKNOWN;
            };
        } catch (Exception e) {
            log.error("[PAYMENT][stripe] reconciliation status check error: {}", e.getMessage());
            return GatewayStatus.UNKNOWN;
        }
    }

    @Override
    public String refund(String transactionId, BigDecimal amount, String currency) {
        if (secretKey.isBlank() || transactionId == null || transactionId.isBlank()) return null;
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String form = "payment_intent=" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8)
                + "&amount=" + minor;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/refunds"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][stripe] refund failed status={} body={}", resp.statusCode(), resp.body());
                return null;
            }
            return objectMapper.readTree(resp.body()).path("id").asText(null);
        } catch (Exception e) {
            log.error("[PAYMENT][stripe] refund error: {}", e.getMessage());
            return null;
        }
    }
}
