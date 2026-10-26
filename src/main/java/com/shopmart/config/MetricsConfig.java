package com.shopmart.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central place to define custom business counters, registered against whichever
 * MeterRegistry is active (Prometheus, via spring-boot-starter-actuator +
 * micrometer-registry-prometheus). Scraped at GET /actuator/prometheus (SUPER_ADMIN-only,
 * see SecurityConfig — point your Prometheus scrape job's bearer token/basic auth at an
 * account with that role).
 */
@Configuration
public class MetricsConfig {

    @Bean
    public AppMetrics appMetrics(MeterRegistry registry) {
        return new AppMetrics(registry);
    }

    public static class AppMetrics {
        private final MeterRegistry registry;

        public AppMetrics(MeterRegistry registry) {
            this.registry = registry;
        }

        public void incrementOrderCreated() {
            registry.counter("shopmart.orders.created").increment();
        }

        public void incrementPaymentResult(String provider, String result) {
            // result: success | failed | refunded
            registry.counter("shopmart.payments.result", "provider", provider, "result", result).increment();
        }

        public void incrementWebhookResult(String provider, String result) {
            // result: verified | rejected
            registry.counter("shopmart.webhooks.result", "provider", provider, "result", result).increment();
        }

        public void incrementUnhandledError(String exceptionType) {
            registry.counter("shopmart.errors.unhandled", "exception", exceptionType).increment();
        }

        public void incrementNotificationFailure(String channel) {
            registry.counter("shopmart.notifications.failed", "channel", channel).increment();
        }
    }
}
