package com.sentinelcore.backend.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the login endpoint, keyed by username and by client IP.
 * State is per application instance; a multi-instance deployment should move this to a shared
 * store or enforce limits at the reverse proxy.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String username, String ip) {
        return blocked(userKey(username)) || blocked(ipKey(ip));
    }

    public void recordFailure(String username, String ip) {
        increment(userKey(username));
        increment(ipKey(ip));
    }

    public void recordSuccess(String username, String ip) {
        attempts.remove(userKey(username));
    }

    private boolean blocked(String key) {
        Attempts a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (a.windowStart.plus(lockDuration).isBefore(clock.instant())) {
            attempts.remove(key);
            return false;
        }
        // IP keys get a larger budget so one office NAT cannot lock everyone out
        int limit = key.startsWith("ip:") ? maxAttempts * 4 : maxAttempts;
        return a.count >= limit;
    }

    private void increment(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.plus(lockDuration).isBefore(now)) {
                return new Attempts(1, now);
            }
            return new Attempts(existing.count + 1, existing.windowStart);
        });
    }

    private static String userKey(String username) {
        return "user:" + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }

    private record Attempts(int count, Instant windowStart) {}
}
