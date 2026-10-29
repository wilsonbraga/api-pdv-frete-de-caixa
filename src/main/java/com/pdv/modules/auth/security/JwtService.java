package com.pdv.modules.auth.security;

import com.pdv.config.PdvProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final PdvProperties pdvProperties;

    public String generateAccessToken(UserPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", principal.getTenantId().toString());
        claims.put("role", principal.getRole());
        claims.put("nome", principal.getNome());
        claims.put("descontoMax", principal.getDescontoMax());
        return buildToken(claims, principal.getId().toString(),
                pdvProperties.getJwt().getExpirationMs());
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(new HashMap<>(), principal.getId().toString(),
                pdvProperties.getJwt().getRefreshExpirationMs());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String userId = extractSubject(token);
            return userId != null && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractSubject(token));
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        pdvProperties.getJwt().getSecret().getBytes()
                )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenExpirationMs() {
        return pdvProperties.getJwt().getExpirationMs();
    }
}
