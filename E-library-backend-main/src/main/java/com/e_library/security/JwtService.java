package com.e_library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {

        this.key =
                Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String email) {

        return Jwts.builder()
                .subject(email)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 15
                        )
                )
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 60 * 24 * 7
                        )
                )
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {

        return extractClaims(token).getSubject();
    }

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {

        try {

            extractClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}