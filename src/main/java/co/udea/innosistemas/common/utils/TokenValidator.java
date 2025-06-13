package co.udea.innosistemas.common.utils;

import co.udea.innosistemas.common.exception.TokenExpiredException;
import co.udea.innosistemas.common.exception.TokenRevokedException;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenValidator {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtils jwtUtils;

    public String validateAndExtractToken(String authHeader) {
        log.debug("Validating authorization header");

        // Validar que el header no sea nulo
        if (authHeader == null) {
            log.warn("Authorization header is missing");
            throw new InvalidTokenException("Header de autorización faltante");
        }

        // Validar formato Bearer
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Invalid authorization header format - missing Bearer prefix");
            throw new InvalidTokenException("Formato de header de autorización inválido");
        }

        // Extraer token
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            log.warn("Token is empty after Bearer prefix");
            throw new InvalidTokenException("Token vacío");
        }

        // Validar token
        validateToken(token);

        log.debug("Token validated successfully");
        return token;
    }

    /**
     * Valida un token sin necesidad del header Bearer
     */
    public void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new InvalidTokenException("Token vacío");
        }

        try {
            // Verificar si está revocado primero
            if (jwtUtils.isTokenRevoked(token)) {
                log.warn("Attempt to use revoked token");
                throw new TokenRevokedException("El token ha sido revocado");
            }

            // Validar como access token
            if (!jwtUtils.isAccessTokenValid(token)) {
                log.warn("Invalid access token provided");
                throw new InvalidTokenException("Token de acceso inválido o expirado");
            }

        } catch (TokenExpiredException ex) {
            log.debug("Token validation failed - expired");
            throw ex;
        } catch (TokenRevokedException ex) {
            log.debug("Token validation failed - revoked");
            throw ex;
        } catch (InvalidTokenException ex) {
            log.debug("Token validation failed - invalid: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during token validation", ex);
            throw new InvalidTokenException("Error interno validando el token");
        }
    }

    /**
     * Valida específicamente un refresh token
     */
    public void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidTokenException("Refresh token vacío");
        }

        try {
            // Verificar si está revocado
            if (jwtUtils.isTokenRevoked(refreshToken)) {
                log.warn("Attempt to use revoked refresh token");
                throw new TokenRevokedException("El refresh token ha sido revocado");
            }

            // Validar como refresh token
            if (jwtUtils.isRefreshTokenValid(refreshToken)) {
                log.warn("Invalid refresh token provided");
                throw new InvalidTokenException("Refresh token inválido o expirado");
            }

        } catch (TokenExpiredException ex) {
            log.debug("Refresh token validation failed - expired");
            throw ex;
        } catch (TokenRevokedException ex) {
            log.debug("Refresh token validation failed - revoked");
            throw ex;
        } catch (InvalidTokenException ex) {
            log.debug("Refresh token validation failed - invalid: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during refresh token validation", ex);
            throw new InvalidTokenException("Error interno validando el refresh token");
        }
    }

    /**
     * Extrae información del token sin validar completamente (útil para logging)
     */
    public String extractEmailSafely(String token) {
        try {
            return jwtUtils.getEmailFromToken(token);
        } catch (Exception ex) {
            log.debug("Could not extract email from token: {}", ex.getMessage());
            return "unknown";
        }
    }

    /**
     * Extrae el rol del token de forma segura
     */
    public String extractRoleSafely(String token) {
        try {
            return jwtUtils.getRoleFromToken(token);
        } catch (Exception ex) {
            log.debug("Could not extract role from token: {}", ex.getMessage());
            return "unknown";
        }
    }
}