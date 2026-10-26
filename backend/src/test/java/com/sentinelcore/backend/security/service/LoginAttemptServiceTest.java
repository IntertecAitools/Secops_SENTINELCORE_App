package com.sentinelcore.backend.security.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    @Test
    void blocksUsernameAfterMaxFailuresAndUnblocksAfterLockPeriod() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);

        for (int i = 0; i < 3; i++) {
            assertFalse(service.isBlocked("Alice", "10.0.0.1"));
            service.recordFailure("alice", "10.0.0.1");
        }
        assertTrue(service.isBlocked("ALICE", "10.0.0.2"), "username key is case-insensitive and IP independent");

        clock.advance(Duration.ofMinutes(16));
        assertFalse(service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    void successResetsUsernameCounter() {
        LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), new MutableClock());
        service.recordFailure("bob", "10.0.0.1");
        service.recordFailure("bob", "10.0.0.1");
        service.recordSuccess("bob", "10.0.0.1");
        service.recordFailure("bob", "10.0.0.1");

        assertFalse(service.isBlocked("bob", "10.0.0.1"));
    }

    @Test
    void blocksIpAfterLargerBudgetAcrossUsernames() {
        LoginAttemptService service = new LoginAttemptService(2, Duration.ofMinutes(15), new MutableClock());
        for (int i = 0; i < 8; i++) {
            service.recordFailure("user" + i, "192.0.2.10");
        }
        assertTrue(service.isBlocked("fresh-user", "192.0.2.10"));
        assertFalse(service.isBlocked("fresh-user", "192.0.2.11"));
    }
}
