package com.actacofrade.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.window-seconds:300}")
    private long windowSeconds;

    @Value("${security.login.lock-seconds:900}")
    private long lockSeconds;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean tryAcquire(String clientKey) {
        Instant now = Instant.now();
        Attempt attempt = attempts.compute(clientKey, (key, existing) -> {
            if (existing == null) {
                return new Attempt(1, now, null);
            }
            if (existing.lockedUntil != null && existing.lockedUntil.isAfter(now)) {
                return existing;
            }
            if (existing.lockedUntil != null && !existing.lockedUntil.isAfter(now)) {
                return new Attempt(1, now, null);
            }
            if (existing.windowStart.plusSeconds(windowSeconds).isBefore(now)) {
                return new Attempt(1, now, null);
            }
            int next = existing.count + 1;
            if (next > maxAttempts) {
                return new Attempt(next, existing.windowStart, now.plusSeconds(lockSeconds));
            }
            return new Attempt(next, existing.windowStart, null);
        });
        return attempt.lockedUntil == null;
    }

    public void recordSuccess(String clientKey) {
        attempts.remove(clientKey);
    }

    private record Attempt(int count, Instant windowStart, Instant lockedUntil) {
    }
}
