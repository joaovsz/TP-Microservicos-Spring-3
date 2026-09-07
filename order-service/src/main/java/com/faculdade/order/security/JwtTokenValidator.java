package com.faculdade.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Apenas tokens com claim token_type == 'ACCESS' são aceitos para autenticar rotas
            String tokenType = claims.get("token_type", String.class);
            return "ACCESS".equalsIgnoreCase(tokenType);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        String role = extractClaims(token).get("role", String.class);
        if (role != null && !role.isBlank()) {
            return List.of(new SimpleGrantedAuthority(role));
        }
        return Collections.emptyList();
    }
}
