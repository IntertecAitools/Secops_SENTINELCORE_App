package com.sentinelcore.backend.security;

import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-key-not-a-secret-0123456789";

    private final User user = User.builder().username("alice").role(Role.AUDITOR).build();

    @Test
    void validTokenRoundTrips() {
        JwtService service = new JwtService(SECRET, 60_000, "sentinelcore", "sentinelcore-api");
        String token = service.generateToken(user);

        assertEquals("alice", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, "alice"));
        assertFalse(service.isTokenValid(token, "bob"));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000, "sentinelcore", "sentinelcore-api");
        String expired = Jwts.builder()
                .subject("alice")
                .issuer("sentinelcore")
                .audience().add("sentinelcore-api").and()
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(JwtException.class, () -> service.parseAndValidate(expired));
        assertFalse(service.isTokenValid(expired, "alice"));
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService issuer = new JwtService("another-signing-key-that-is-long-enough-123456", 60_000,
                "sentinelcore", "sentinelcore-api");
        JwtService verifier = new JwtService(SECRET, 60_000, "sentinelcore", "sentinelcore-api");

        assertThrows(JwtException.class, () -> verifier.parseAndValidate(issuer.generateToken(user)));
    }

    @Test
    void wrongIssuerOrAudienceIsRejected() {
        JwtService verifier = new JwtService(SECRET, 60_000, "sentinelcore", "sentinelcore-api");
        String wrongIssuer = new JwtService(SECRET, 60_000, "someone-else", "sentinelcore-api").generateToken(user);
        String wrongAudience = new JwtService(SECRET, 60_000, "sentinelcore", "other-api").generateToken(user);

        assertThrows(JwtException.class, () -> verifier.parseAndValidate(wrongIssuer));
        assertThrows(JwtException.class, () -> verifier.parseAndValidate(wrongAudience));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000, "sentinelcore", "sentinelcore-api");
        String token = service.generateToken(user);
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"superadmin\",\"iss\":\"sentinelcore\",\"aud\":\"sentinelcore-api\",\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> service.parseAndValidate(forged));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 60_000, "sentinelcore", "sentinelcore-api"));
    }

    @Test
    void missingSecretUsesEphemeralKey() {
        JwtService first = new JwtService("", 60_000, "sentinelcore", "sentinelcore-api");
        JwtService second = new JwtService("", 60_000, "sentinelcore", "sentinelcore-api");
        String token = first.generateToken(user);

        assertEquals("alice", first.extractUsername(token));
        // A different process (new ephemeral key) cannot validate the token
        assertThrows(JwtException.class, () -> second.parseAndValidate(token));
    }
}
