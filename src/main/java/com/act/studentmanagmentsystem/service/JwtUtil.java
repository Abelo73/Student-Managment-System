package com.act.studentmanagmentsystem.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        logger.info("Generating JWT for email: {}", email);
        try {
            return Jwts.builder()
                    .subject(email)
                    .claim("role", role)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            logger.error("Failed to generate JWT for email: {}", email, e);
            throw new RuntimeException("JWT generation failed", e);
        }
    }

    public String extractEmail(String token) {
        logger.debug("Extracting email from JWT: {}", token);
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract email from JWT", e);
            throw new RuntimeException("JWT parsing failed", e);
        }
    }

    public String extractRole(String token) {
        logger.debug("Extracting role from JWT: {}", token);
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role", String.class);
        } catch (Exception e) {
            logger.error("Failed to extract role from JWT", e);
            throw new RuntimeException("JWT parsing failed", e);
        }
    }

    public boolean validateToken(String token) {
        logger.debug("Validating JWT: {}", token);
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            logger.info("JWT validation successful");
            return true;
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}