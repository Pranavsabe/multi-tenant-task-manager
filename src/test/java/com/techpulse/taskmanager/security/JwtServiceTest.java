package com.techpulse.taskmanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "dGVjaHB1bHNlLWl0LW11bHRpLXRlbmFudC10YXNrLW1hbmFnZXItc3VwZXItc2VjcmV0LWtleS0yMDI2LWFzc2lnbm1lbnQ=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 3_600_000L);
    }

    @Test
    void generateToken_embedsAllRequiredClaims() {
        String token = jwtService.generateToken(1L, "admin@acme.com", 10L, "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.extractTenantId(token)).isEqualTo(10L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@acme.com");
    }

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken(2L, "u@acme.com", 5L, "USER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(secret, -1000L);
        String token = shortLivedJwtService.generateToken(3L, "x@acme.com", 7L, "MANAGER");
        assertThat(shortLivedJwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }
}
