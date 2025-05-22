package co.udea.innosistemas.common.utils;

import co.udea.innosistemas.auth.model.RevokedToken;
import co.udea.innosistemas.auth.repository.RevokedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.ZoneOffset;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final long REFRESH_TOKEN_DURATION = 7 * 24 * 60 * 60 * 1000L; // 7 days

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final RevokedTokenRepository revokedTokenRepository;
    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return generateToken(email, expiration, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, REFRESH_TOKEN_DURATION, TOKEN_TYPE_REFRESH);
    }

    private String generateToken(String email, long duration, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + duration);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("type", type)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, TOKEN_TYPE_ACCESS);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, TOKEN_TYPE_REFRESH);
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            if (revokedTokenRepository.existsByToken(token)) {
                return false;
            }

            Claims claims = parseToken(token);
            String tokenType = claims.get("type", String.class);
            return expectedType.equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    public void revokeToken(String token, String tokenType) {
        Claims claims = parseToken(token);
        RevokedToken revoked = RevokedToken.builder()
                .token(token)
                .userEmail(claims.getSubject())
                .expiration(claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC))
                .tokenType(tokenType)
                .build();

        revokedTokenRepository.save(revoked);
    }

    public long getExpirationInSeconds() {
        return expiration / 1000;
    }
}