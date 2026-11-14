package com.rodrigogalvao.partnermock.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "demo-only-jwt-secret-change-me-in-production-1234567890";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET);

    @Test
    void validateTokenReturnsFalseForGarbageInput() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForTokenSignedWithDifferentSecret() {
        SecretKey foreignKey = Keys.hmacShaKeyFor(
                "another-secret-another-secret-another-secret!".getBytes());
        String foreignToken = Jwts.builder()
                .subject("transaction-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(foreignKey)
                .compact();

        assertThat(provider.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateTokenReturnsTrueForWellFormedTokenSignedWithSharedSecret() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .subject("transaction-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThat(provider.validateToken(token)).isTrue();
    }
}
