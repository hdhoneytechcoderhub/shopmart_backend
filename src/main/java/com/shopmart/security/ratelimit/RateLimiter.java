package com.shopmart.security.ratelimit;

/**
 * Fixed-window rate limiter used to protect sensitive endpoints (login, OTP) from
 * brute force / credential-stuffing. Two implementations are wired in, mirroring the
 * dual strategy already used for {@code CacheConfig}:
 *  - {@link InMemoryRateLimiter}: default, no external dependency, per-instance only.
 *  - {@link RedisRateLimiter}: active under the "redis" profile, shared across instances —
 *    required for correct limiting once the app runs with more than one replica.
 */
public interface RateLimiter {

    /**
     * Registers one attempt for {@code key} and reports whether the caller is currently
     * allowed to proceed.
     *
     * @param key            unique bucket key, e.g. "login:ip:email"
     * @param maxAttempts    attempts allowed within the window
     * @param windowSeconds  window length in seconds
     * @return true if this attempt is within the allowed limit, false if the caller should be blocked
     */
    boolean tryConsume(String key, int maxAttempts, int windowSeconds);

    /** Clears the bucket for {@code key}, e.g. after a successful login. */
    void reset(String key);
}
