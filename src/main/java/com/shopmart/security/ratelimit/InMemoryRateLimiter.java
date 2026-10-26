package com.shopmart.security.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default rate limiter: in-memory fixed window, no external dependency.
 * Correct for a single instance; NOT shared across Railway replicas if the app scales
 * horizontally — in that case activate the "redis" profile so RedisRateLimiter takes over.
 * A background sweep isn't implemented; stale buckets are evicted lazily on next access,
 * which is fine at auth-endpoint volumes and avoids adding a scheduler for this alone.
 */
@Slf4j
@Component
@Profile("!redis")
@RequiredArgsConstructor
public class InMemoryRateLimiter implements RateLimiter {

    private final MeterRegistry meterRegistry;

//    private record Bucket(AtomicInteger count, volatile long windowStartEpochSec) {}

    private record Bucket(
            AtomicInteger count,
            long windowStartEpochSec
    ) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, int maxAttempts, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartEpochSec() >= windowSeconds) {
                return new Bucket(new AtomicInteger(0), now);
            }
            return existing;
        });
        int attempts = bucket.count().incrementAndGet();
        boolean allowed = attempts <= maxAttempts;
        if (!allowed) {
            log.warn("[RATE-LIMIT] blocked key={} attempts={} max={}", key, attempts, maxAttempts);
            meterRegistry.counter("shopmart.ratelimit.blocked", "bucket", bucketType(key)).increment();
        }
        return allowed;
    }

    @Override
    public void reset(String key) {
        buckets.remove(key);
    }

    private String bucketType(String key) {
        int idx = key.indexOf(':');
        return idx > 0 ? key.substring(0, idx) : key;
    }
}
