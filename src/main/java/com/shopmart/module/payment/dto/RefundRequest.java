package com.shopmart.module.payment.dto;

import java.math.BigDecimal;

/**
 * amount:         null => refund the full remaining refundable balance. Otherwise a partial
 *                 refund for exactly this amount (validated against what's left to refund).
 * idempotencyKey: caller-supplied token (e.g. a UUID generated once per admin click). Retrying
 *                 the same request with the same key returns the original result instead of
 *                 issuing a second refund — protects against double-clicks and network retries.
 */
public record RefundRequest(BigDecimal amount, String idempotencyKey) {}
