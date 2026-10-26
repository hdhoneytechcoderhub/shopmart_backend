package com.shopmart.security.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed fixed-window rate limiter, active under --spring.profiles.active=redis.
 * Shared across all app instances, so limits hold correctly once Railway is scaled to
 * more than one replica. Reuses the same Redis connection factory as CacheConfig.
 */
@Slf4j
@Component
@Profile("redis")
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private static final String PREFIX = "ratelimit:";

    @Override
    public boolean tryConsume(String key, int maxAttempts, int windowSeconds) {
        String redisKey = PREFIX + key;
        Long attempts = redisTemplate.opsForValue().increment(redisKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        boolean allowed = attempts != null && attempts <= maxAttempts;
        if (!allowed) {
            log.warn("[RATE-LIMIT] blocked key={} attempts={} max={}", key, attempts, maxAttempts);
            meterRegistry.counter("shopmart.ratelimit.blocked", "bucket", bucketType(key)).increment();
        }
        return allowed;
    }

    @Override
    public void reset(String key) {
        redisTemplate.delete(PREFIX + key);
    }

    private String bucketType(String key) {
        int idx = key.indexOf(':');
        return idx > 0 ? key.substring(0, idx) : key;
    }
}
