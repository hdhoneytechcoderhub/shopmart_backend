package com.shopmart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.common.notification.EmailSender;
import com.shopmart.common.notification.LoggingEmailSender;
import com.shopmart.common.notification.ResendEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Picks the mail channel at startup.
 *
 * <p>With RESEND_API_KEY + RESEND_FROM_EMAIL set, mail is delivered through Resend.
 * Without them, a logging sender is used so local development (signup OTPs, order
 * emails) works without external credentials instead of failing every request that
 * needs to send an email.</p>
 */
@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailSender emailSender(ObjectMapper objectMapper,
                                   @Value("${app.resend.api-key:}") String apiKey,
                                   @Value("${app.resend.from:}") String from) {
        if (apiKey.isBlank() || from.isBlank()) {
            log.warn("Resend is not configured (RESEND_API_KEY / RESEND_FROM_EMAIL). " +
                    "Emails will be logged, not delivered.");
            return new LoggingEmailSender();
        }
        log.info("Email delivery configured through Resend (from={})", from);
        return new ResendEmailSender(objectMapper, apiKey, from);
    }
}
