package com.shopmart.common.notification;

import lombok.extern.slf4j.Slf4j;

/**
 * Development fallback used when Resend is not configured (no RESEND_API_KEY /
 * RESEND_FROM_EMAIL). Logs the message instead of delivering it, so signup OTPs and
 * order emails keep working locally without external credentials.
 */
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) return;
        log.info("[EMAIL:log] Resend is not configured; email not delivered. to={} subject={}\n{}",
                to, subject, body);
    }
}
