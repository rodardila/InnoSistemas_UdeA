package co.udea.innosistemas.common.utils;

import co.udea.innosistemas.common.exception.TokenExpiredException;
import co.udea.innosistemas.common.exception.TokenRevokedException;
import co.udea.innosistemas.auth.model.RevokedToken;
import co.udea.innosistemas.auth.repository.RevokedTokenRepository;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
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

    public String generateToken(String email, String role) {
        return generateToken(email, expiration, TOKEN_TYPE_ACCESS, role);
    }

    public String generateRefreshToken(String email, String role) {
        return generateToken(email, REFRESH_TOKEN_DURATION, TOKEN_TYPE_REFRESH, role);
    }

    private String generateToken(String email, long duration, String type, String role) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + duration);

            return Jwts.builder()
                    .setSubject(email)
                    .setIssuedAt(now)
                    .setExpiration(expiry)
                    .claim("type", type)
                    .claim("role", role != null ? role : "")
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception ex) {
            log.error("Error generating {} token for user: {}", type, email, ex);
            throw new InvalidTokenException("Error al generar el token");
        }
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            log.debug("Token expired: {}", ex.getMessage());
            throw new TokenExpiredException("El token ha expirado");
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
            throw new InvalidTokenException("Token no soportado");
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
            throw new InvalidTokenException("Token malformado");
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
            throw new InvalidTokenException("Firma del token inválida");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT token compact of handler are invalid: {}", ex.getMessage());
            throw new InvalidTokenException("Token inválido");
        } catch (Exception ex) {
            log.error("Unexpected error parsing token", ex);
            throw new InvalidTokenException("Error al procesar el token");
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (TokenExpiredException | InvalidTokenException ex) {
            // Re-lanzar excepciones específicas
            throw ex;
        } catch (Exception ex) {
            log.error("Error extracting email from token", ex);
            throw new InvalidTokenException("Error al extraer información del token");
        }
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, TOKEN_TYPE_ACCESS);
    }

    public boolean isRefreshTokenValid(String token) {
        return !isTokenValid(token, TOKEN_TYPE_REFRESH);
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            // Verificar si el token está revocado
            if (revokedTokenRepository.existsByToken(token)) {
                log.debug("Token is revoked");
                return false;
            }

            // Parsear y validar el token
            Claims claims = parseToken(token);
            String tokenType = claims.get("type", String.class);

            boolean isValidType = expectedType.equals(tokenType);
            if (!isValidType) {
                log.debug("Token type mismatch. Expected: {}, Found: {}", expectedType, tokenType);
            }

            return isValidType;

        } catch (TokenExpiredException ex) {
            log.debug("Token validation failed - expired: {}", ex.getMessage());
            return false;
        } catch (InvalidTokenException ex) {
            log.debug("Token validation failed - invalid: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Unexpected error during token validation", ex);
            return false;
        }
    }

    public void revokeToken(String token, String tokenType) {
        try {
            // Verificar si ya está revocado
            if (revokedTokenRepository.existsByToken(token)) {
                log.debug("Token already revoked");
                throw new TokenRevokedException("El token ya ha sido revocado");
            }

            Claims claims = parseToken(token);
            RevokedToken revoked = RevokedToken.builder()
                    .token(token)
                    .userEmail(claims.getSubject())
                    .expiration(claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC))
                    .tokenType(tokenType)
                    .revokedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            revokedTokenRepository.save(revoked);
            log.debug("Token revoked successfully for user: {}", claims.getSubject());

        } catch (TokenRevokedException ex) {
            // Re-lanzar excepción específica
            throw ex;
        } catch (TokenExpiredException ex) {
            log.warn("Attempt to revoke expired token");
            throw new InvalidTokenException("No se puede revocar un token expirado");
        } catch (InvalidTokenException ex) {
            log.warn("Attempt to revoke invalid token");
            throw ex;
        } catch (Exception ex) {
            log.error("Error revoking token", ex);
            throw new InvalidTokenException("Error al revocar el token");
        }
    }

    public long getExpirationInSeconds() {
        return expiration / 1000;
    }

    /**
     * Verifica si un token está específicamente revocado
     */
    public boolean isTokenRevoked(String token) {
        try {
            return revokedTokenRepository.existsByToken(token);
        } catch (Exception ex) {
            log.error("Error checking token revocation status", ex);
            return false; // En caso de error, asumimos que no está revocado para no bloquear al usuario
        }
    }

    /**
     * Obtiene el rol del token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (TokenExpiredException | InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error extracting role from token", ex);
            throw new InvalidTokenException("Error al extraer el rol del token");
        }
    }

    /**
     * Obtiene el tipo de token
     */
    public String getTokenType(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("type", String.class);
        } catch (TokenExpiredException | InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error extracting token type", ex);
            throw new InvalidTokenException("Error al extraer el tipo de token");
        }
    }

    /**
     * Obtiene la fecha de expiración del token
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration();
        } catch (TokenExpiredException | InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error extracting expiration date from token", ex);
            throw new InvalidTokenException("Error al extraer la fecha de expiración del token");
        }
    }
}