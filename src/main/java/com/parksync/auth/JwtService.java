package com.parksync.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Servicio JWT con TTL extendido de 12 h (RTF-01, NFR-10).
 * Secreto inyectado por variable de entorno — nunca hardcodeado (Design Doc §12.2).
 */
@Service
public class JwtService {

    private static final long TTL_MS = 12L * 60 * 60 * 1000; // 12 horas

    @Value("${parksync.jwt.secret}")
    private String jwtSecret;

    public String generateToken(String username, String parkingLotId, String role) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("parkingLotId", parkingLotId, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TTL_MS))
                .signWith(getKey())
                .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
