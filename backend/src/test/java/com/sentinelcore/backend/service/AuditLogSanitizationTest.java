package com.sentinelcore.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogSanitizationTest {

    @Test
    void redactsCredentialLikeValues() {
        String sanitized = AuditLogService.sanitize(
                "args: {newPassword=Hunter2!, token: abc.def.ghi, apiKey=AKIAEXAMPLE, note=ok}", 2000);

        assertFalse(sanitized.contains("Hunter2!"));
        assertFalse(sanitized.contains("abc.def.ghi"));
        assertFalse(sanitized.contains("AKIAEXAMPLE"));
        assertTrue(sanitized.contains("note=ok"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }

    @Test
    void stripsLineBreaksToPreventLogForging() {
        String sanitized = AuditLogService.sanitize("line1\r\nFAKE_ENTRY admin", 2000);
        assertFalse(sanitized.contains("\n"));
        assertFalse(sanitized.contains("\r"));
    }

    @Test
    void truncatesLongValues() {
        String sanitized = AuditLogService.sanitize("x".repeat(5000), 100);
        assertTrue(sanitized.length() <= 101);
    }
}
