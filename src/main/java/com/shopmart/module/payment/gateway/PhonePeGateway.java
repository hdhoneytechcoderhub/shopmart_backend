package com.shopmart.module.payment.gateway;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.util.HmacUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * PhonePe PG (UPI / cards / wallets) gateway.
 * Active when app.payments.provider=phonepe.
 *
 * PhonePe authenticates each call with an X-VERIFY checksum:
 *   X-VERIFY = SHA256(base64Payload + apiPath + saltKey) + "###" + saltIndex
 * The callback is verified with the same scheme. Set the merchant id, salt key,
 * and salt index via env vars before going live.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "phonepe")
public class PhonePeGateway implements PaymentGateway {

    private static final String PAY_PATH = "/pg/v1/pay";
    private static final String STATUS_BASE_URL = "https://api.phonepe.com/apis/hermes";

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String merchantId;
    private final String saltKey;
    private final String saltIndex;

    public PhonePeGateway(@Value("${app.payments.phonepe.merchant-id:}") String merchantId,
                          @Value("${app.payments.phonepe.salt-key:}") String saltKey,
                          @Value("${app.payments.phonepe.salt-index:1}") String saltIndex) {
        this.merchantId = merchantId;
        this.saltKey = saltKey;
        this.saltIndex = saltIndex;
    }

    @Override
    public String provider() {
        return "phonepe";
    }

    @Override
    public String createGatewayOrder(Long orderId, BigDecimal amount, String currency) {
        if (merchantId.isBlank() || saltKey.isBlank()) {
            throw new BadRequestException("PhonePe credentials are not configured");
        }
        long minor = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String merchantTxnId = "order_" + orderId + "_" + System.currentTimeMillis();

        // Build the request payload PhonePe expects (base64-encoded JSON).
        String payload = "{"
                + "\"merchantId\":\"" + merchantId + "\","
                + "\"merchantTransactionId\":\"" + merchantTxnId + "\","
                + "\"amount\":" + minor + ","
                + "\"paymentInstrument\":{\"type\":\"PAY_PAGE\"}"
                + "}";
        String base64Payload = Base64.getEncoder()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        // X-VERIFY checksum (computed here so the frontend/caller can use it).
        String xVerify = checksum(base64Payload, PAY_PATH);
        log.info("[PAYMENT][phonepe] created intent txn={} xVerifyLen={}", merchantTxnId, xVerify.length());

        // The actual HTTPS POST to PhonePe is performed by the checkout client with the
        // base64Payload + X-VERIFY header. We return our merchant transaction id as the ref.
        return merchantTxnId;
    }

    @Override
    public boolean verifySignature(String gatewayRef, String transactionId, String signature) {
        // PhonePe callback X-VERIFY = SHA256(base64Response + saltKey) + "###" + saltIndex.
        // Here transactionId carries the base64 response body from the callback.
        String expected = sha256Hex(transactionId + saltKey) + "###" + saltIndex;
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][phonepe] verify ref={} -> {}", gatewayRef, ok);
        return ok;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // PhonePe server-to-server callback: X-VERIFY = SHA256(payload + saltKey) + "###" + saltIndex.
        // `payload` here is the raw callback request body (base64 response envelope from PhonePe).
        if (saltKey.isBlank()) {
            log.error("[PAYMENT][phonepe] salt key not configured - rejecting webhook (fail-closed)");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[PAYMENT][phonepe] webhook missing X-Verify header - rejecting");
            return false;
        }
        String expected = sha256Hex(payload + saltKey) + "###" + saltIndex;
        boolean ok = HmacUtil.constantTimeEquals(expected, signature);
        log.info("[PAYMENT][phonepe] webhook signature verify -> {}", ok);
        return ok;
    }

    @Override
    public GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        // PhonePe Check Status API: GET /pg/v1/status/{merchantId}/{merchantTransactionId}
        // X-VERIFY = SHA256(apiPath + saltKey) + "###" + saltIndex (no payload body on this call).
        if (merchantId.isBlank() || saltKey.isBlank()) return GatewayStatus.UNKNOWN;
        String apiPath = "/pg/v1/status/" + merchantId + "/" + gatewayRef;
        String xVerify = sha256Hex(apiPath + saltKey) + "###" + saltIndex;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(STATUS_BASE_URL + apiPath))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-VERIFY", xVerify)
                    .header("X-MERCHANT-ID", merchantId)
                    .header("Content-Type", "application/json")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("[PAYMENT][phonepe] reconciliation status check failed status={}", resp.statusCode());
                return GatewayStatus.UNKNOWN;
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String code = node.path("code").asText("");
            return switch (code) {
                case "PAYMENT_SUCCESS" -> GatewayStatus.SUCCESS;
                case "PAYMENT_ERROR", "PAYMENT_DECLINED" -> GatewayStatus.FAILED;
                case "PAYMENT_PENDING" -> GatewayStatus.PENDING;
                default -> GatewayStatus.UNKNOWN;
            };
        } catch (Exception e) {
            log.error("[PAYMENT][phonepe] reconciliation status check error: {}", e.getMessage());
            return GatewayStatus.UNKNOWN;
        }
    }

    private String checksum(String base64Payload, String apiPath) {
        return sha256Hex(base64Payload + apiPath + saltKey) + "###" + saltIndex;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new BadRequestException("PhonePe checksum error");
        }
    }
}
