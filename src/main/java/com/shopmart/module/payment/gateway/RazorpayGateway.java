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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Razorpay Orders API + checkout signature verification.
 * Active when app.payments.provider=razorpay. Uses the JDK HTTP client (no SDK dependency).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayGateway(ObjectMapper objectMapper,
                           @Value("${app.payments.razorpay.key-id:}") String keyId,
                           @Value("${app.payments.razorpay.key-secret:}") String keySecret,
                           @Value("${app.payments.razorpay.webhook-secret:}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String provider() {
        return "razorpay";
    }

    @Override
    public String publicKey() {
        return keyId;
    }

    @Override
    public String createGatewayOrder(Long orderId, BigDecimal amount, String currency) {
        log.info("Creating Razorpay Order for order {} amount {}", orderId, amount);
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new BadRequestException("Razorpay credentials are not configured");
        }
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String body = "{\"amount\":" + minor + ",\"currency\":\"" + currency
                + "\",\"receipt\":\"order_" + orderId + "\"}";
        String auth = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ORDERS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][razorpay] order create failed status={} body={}", resp.statusCode(), resp.body());
                throw new BadRequestException("Razorpay order creation failed");
            }
            JsonNode node = objectMapper.readTree(resp.body());
            return node.path("id").asText();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] order create error: {}", e.getMessage());
            throw new BadRequestException("Razorpay order creation error");
        }
    }

    @Override
    public boolean verifySignature(String gatewayRef, String transactionId, String signature) {
        log.info("Payment Verification Started ref={}", gatewayRef);
        // Razorpay checkout signature = HMAC_SHA256(order_id + "|" + payment_id, key_secret)
        String expected = HmacUtil.hmacSha256Hex(gatewayRef + "|" + transactionId, keySecret);
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][razorpay] verify ref={} txn={} -> {}", gatewayRef, transactionId, ok);
        return ok;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // Razorpay webhook signature = HMAC_SHA256(raw request body, webhook secret),
        // sent in the X-Razorpay-Signature header. Distinct secret from the checkout key-secret.
        if (webhookSecret.isBlank()) {
            log.error("[PAYMENT][razorpay] webhook secret not configured - rejecting webhook (fail-closed)");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[PAYMENT][razorpay] webhook missing X-Razorpay-Signature header - rejecting");
            return false;
        }
        String expected = HmacUtil.hmacSha256Hex(payload, webhookSecret);
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][razorpay] webhook signature verify -> {}", ok);
        return ok;
    }

    @Override
    public GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        // Prefer the specific payment id when we have one (captured after checkout);
        // otherwise fall back to inspecting the order's status.
        try {
            String path = (transactionId != null && !transactionId.isBlank())
                    ? "/v1/payments/" + transactionId
                    : "/v1/orders/" + gatewayRef;
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.razorpay.com" + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("[PAYMENT][razorpay] reconciliation status check failed status={}", resp.statusCode());
                return GatewayStatus.UNKNOWN;
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String status = node.path("status").asText("");
            return switch (status) {
                case "captured", "paid" -> GatewayStatus.SUCCESS;
                case "failed" -> GatewayStatus.FAILED;
                case "created", "attempted" -> GatewayStatus.PENDING;
                default -> GatewayStatus.UNKNOWN;
            };
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] reconciliation status check error: {}", e.getMessage());
            return GatewayStatus.UNKNOWN;
        }
    }

    @Override
    public String refund(String transactionId, BigDecimal amount, String currency) {
        if (keySecret.isBlank() || transactionId == null || transactionId.isBlank()) return null;
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String auth = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        try {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create("https://api.razorpay.com/v1/payments/" + transactionId + "/refund"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":" + minor + "}"))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][razorpay] refund failed status={} body={}", resp.statusCode(), resp.body());
                return null;
            }
            return objectMapper.readTree(resp.body()).path("id").asText(null);
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] refund error: {}", e.getMessage());
            return null;
        }
    }
}
