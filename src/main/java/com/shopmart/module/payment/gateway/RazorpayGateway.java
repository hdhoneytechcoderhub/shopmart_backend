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
 * Razorpay Orders API, authoritative payment lookup, capture and signature checks.
 * Active when app.payments.provider=razorpay. Uses the JDK HTTP client (no SDK dependency).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {

    private static final String API_BASE = "https://api.razorpay.com";
    private static final String ORDERS_URL = API_BASE + "/v1/orders";

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

    private boolean credentialsConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    private String authHeader() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createGatewayOrder(Long orderId, BigDecimal amount, String currency) {
        log.info("Creating Razorpay order for order {} amount {}", orderId, amount);
        if (!credentialsConfigured()) {
            // Fail closed: never let checkout proceed without real gateway credentials.
            throw new BadRequestException("Razorpay is not configured (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET)");
        }
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String body = "{\"amount\":" + minor
                + ",\"currency\":\"" + currency
                + "\",\"receipt\":\"order_" + orderId + "\""
                + ",\"notes\":{\"shopmartOrderId\":\"" + orderId + "\"}}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ORDERS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][razorpay] order create failed status={} body={}", resp.statusCode(), resp.body());
                throw new BadRequestException("Razorpay order creation failed");
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String id = node.path("id").asText();
            if (id.isBlank()) {
                throw new BadRequestException("Razorpay order creation returned no order id");
            }
            return id;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] order create error: {}", e.getMessage());
            throw new BadRequestException("Razorpay order creation error");
        }
    }

    @Override
    public boolean verifySignature(String gatewayRef, String transactionId, String signature) {
        if (!credentialsConfigured()) {
            log.error("[PAYMENT][razorpay] key secret not configured - cannot verify checkout signature");
            return false;
        }
        if (gatewayRef == null || transactionId == null || signature == null) return false;
        // Razorpay checkout signature = HMAC_SHA256(order_id + "|" + payment_id, key_secret)
        String expected = HmacUtil.hmacSha256Hex(gatewayRef + "|" + transactionId, keySecret);
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][razorpay] checkout signature verify order={} payment={} -> {}",
                gatewayRef, transactionId, ok);
        return ok;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // Razorpay webhook signature = HMAC_SHA256(raw request body, webhook secret),
        // sent in the X-Razorpay-Signature header. Distinct secret from the checkout key-secret.
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[PAYMENT][razorpay] webhook secret not configured - rejecting webhook (fail-closed)");
            return false;
        }
        if (payload == null || signature == null || signature.isBlank()) {
            log.warn("[PAYMENT][razorpay] webhook missing payload or X-Razorpay-Signature - rejecting");
            return false;
        }
        String expected = HmacUtil.hmacSha256Hex(payload, webhookSecret);
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][razorpay] webhook signature verify -> {}", ok);
        return ok;
    }

    /**
     * Fetch the authoritative state of a payment. If a payment id is known it is queried
     * directly; otherwise the order's payment list is searched for a captured payment.
     * Any transport/parse failure returns null so callers fail closed.
     */
    @Override
    public GatewayPayment fetchPayment(String gatewayRef, String transactionId) {
        if (!credentialsConfigured() || gatewayRef == null || gatewayRef.isBlank()) return null;
        try {
            if (transactionId != null && !transactionId.isBlank()) {
                JsonNode payment = getJson("/v1/payments/" + transactionId);
                if (payment == null) return null;
                String orderId = payment.path("order_id").asText("");
                if (!gatewayRef.equals(orderId)) {
                    log.warn("[PAYMENT][razorpay] payment {} belongs to order {}, not {}",
                            transactionId, orderId, gatewayRef);
                    return null;
                }
                return toGatewayPayment(payment);
            }

            JsonNode order = getJson("/v1/orders/" + gatewayRef);
            if (order == null) return null;
            String orderStatus = order.path("status").asText("");
            if ("paid".equals(orderStatus)) {
                JsonNode payments = getJson("/v1/orders/" + gatewayRef + "/payments");
                if (payments != null) {
                    JsonNode items = payments.path("items");
                    JsonNode chosen = null;
                    // Prefer a captured payment; fall back to an authorised one.
                    for (JsonNode p : items) {
                        if (p.path("captured").asBoolean(false) || "captured".equals(p.path("status").asText(""))) {
                            chosen = p;
                            break;
                        }
                    }
                    if (chosen == null) {
                        for (JsonNode p : items) {
                            if ("authorized".equals(p.path("status").asText(""))) { chosen = p; break; }
                        }
                    }
                    if (chosen == null && items.isArray() && items.size() > 0) chosen = items.get(0);
                    if (chosen != null) return toGatewayPayment(chosen);
                }
                // Order says paid but we could not load the payment: unknown, not success.
                return new GatewayPayment(gatewayRef, null, GatewayStatus.UNKNOWN, null, null, null,
                        "paid order has no readable payment");
            }
            GatewayStatus status = switch (orderStatus) {
                case "attempted" -> GatewayStatus.PENDING;
                case "created" -> GatewayStatus.PENDING;
                default -> GatewayStatus.UNKNOWN;
            };
            return new GatewayPayment(gatewayRef, null, status, null, null, null, null);
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] payment lookup error: {}", e.getMessage());
            return null;
        }
    }

    private GatewayPayment toGatewayPayment(JsonNode node) {
        String paymentId = node.path("id").asText(null);
        String orderId = node.path("order_id").asText(null);
        String status = node.path("status").asText("");
        boolean captured = node.path("captured").asBoolean(false);
        long minor = node.path("amount").asLong(0);
        BigDecimal amount = minor > 0 ? BigDecimal.valueOf(minor).movePointLeft(2) : null;
        String currency = node.path("currency").asText(null);
        String method = node.path("method").asText(null);
        String failure = node.path("error_description").asText(null);

        GatewayStatus mapped;
        if (captured || "captured".equals(status) || "paid".equals(status)) {
            mapped = GatewayStatus.SUCCESS;
        } else if ("authorized".equals(status)) {
            mapped = GatewayStatus.AUTHORIZED;
        } else if ("failed".equals(status) || "cancelled".equals(status)) {
            mapped = GatewayStatus.FAILED;
        } else if ("created".equals(status) || "attempted".equals(status) || "pending".equals(status)) {
            mapped = GatewayStatus.PENDING;
        } else {
            mapped = GatewayStatus.UNKNOWN;
        }
        return new GatewayPayment(orderId, paymentId, mapped, amount, currency, method, failure);
    }

    @Override
    public boolean capturePayment(String paymentId, BigDecimal amount, String currency) {
        if (!credentialsConfigured() || paymentId == null || paymentId.isBlank()) return false;
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String body = "{\"amount\":" + minor + ",\"currency\":\"" + currency + "\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_BASE + "/v1/payments/" + paymentId + "/capture"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.error("[PAYMENT][razorpay] capture failed status={} body={}", resp.statusCode(), resp.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] capture error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        GatewayPayment payment = fetchPayment(gatewayRef, transactionId);
        return payment == null ? GatewayStatus.UNKNOWN : payment.status();
    }

    private JsonNode getJson(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_BASE + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader())
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("[PAYMENT][razorpay] GET {} failed status={}", path, resp.statusCode());
                return null;
            }
            return objectMapper.readTree(resp.body());
        } catch (Exception e) {
            log.error("[PAYMENT][razorpay] GET {} error: {}", path, e.getMessage());
            return null;
        }
    }

    @Override
    public String refund(String transactionId, BigDecimal amount, String currency) {
        if (!credentialsConfigured() || transactionId == null || transactionId.isBlank()) return null;
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        try {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(API_BASE + "/v1/payments/" + transactionId + "/refund"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader())
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
