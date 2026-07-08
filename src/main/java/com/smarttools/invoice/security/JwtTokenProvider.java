package com.smarttools.invoice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate Access Token (short-lived, 15m) containing userId and companyId.
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal, accessTokenExpirationMs);
    }

    /**
     * Generate Refresh Token (long-lived, 7d).
     */
    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal, refreshTokenExpirationMs);
    }

    private String generateToken(UserPrincipal userPrincipal, long expiryMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", userPrincipal.getName());
        claims.put("role", userPrincipal.getRole());

        return Jwts.builder()
            .subject(Long.toString(userPrincipal.getId()))
            .claims(claims)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    /**
     * Extract User ID (subject) from JWT.
     */
    public Long getUserIdFromJwt(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract Role from JWT.
     */
    public String getRoleFromJwt(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * Validate JWT Token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }
}
