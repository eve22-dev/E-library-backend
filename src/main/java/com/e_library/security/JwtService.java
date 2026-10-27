package com.e_library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // O papel (aluno, professor ou bibliotecario) vai dentro do token
    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "access")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 15))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = extractClaims(token);

        String email = claims.get("email", String.class);

        if (email == null) {
            email = claims.getSubject();
        }

        return email;
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

    private boolean hasType(String token, String type) {
        try {
            return type.equals(extractClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // Só token de ACESSO abre a API
    public boolean isAccessToken(String token) {
        return hasType(token, "access");
    }

    // Só token de REFRESH renova a sessão
    public boolean isRefreshToken(String token) {
        return hasType(token, "refresh");
    }
}
