package com.sentinelcore.backend.security.jwt;

import com.sentinelcore.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 requires >= 256-bit keys

    private final SecretKey key;
    private final long expirationMillis;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMillis,
            @Value("${jwt.issuer:sentinelcore}") String issuer,
            @Value("${jwt.audience:sentinelcore-api}") String audience) {

        if (StringUtils.hasText(secret)) {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long");
            }
            this.key = Keys.hmacShaKeyFor(secretBytes);
        } else {
            // No secret configured: use a random per-process key so no signing key lives in source.
            // Tokens become invalid on restart; production deployments must set JWT_SECRET.
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SECRET is not configured. Using an ephemeral signing key; "
                    + "all sessions will be invalidated on restart. Set JWT_SECRET in production.");
        }

        if (expirationMillis <= 0) {
            throw new IllegalStateException("jwt.expiration must be positive");
        }
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(User user) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getRole().name());
        user.getRole().getPermissions().forEach(permission -> authorities.add(permission.name()));

        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .audience().add(audience).and()
                // role/authorities are informational for the UI; the backend reloads them from the database
                .claim("role", user.getRole().name())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature, expiry, issuer and audience. Throws {@link JwtException} when any check fails.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseAndValidate(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseAndValidate(token);
            return username != null
                    && username.equals(claims.getSubject())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
