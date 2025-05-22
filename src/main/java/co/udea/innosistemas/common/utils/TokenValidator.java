package co.udea.innosistemas.common.utils;

import co.udea.innosistemas.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenValidator {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtils jwtUtils;
    
    public String validateAndExtractToken(String authHeader) {
        if (authHeader == null) {
            throw new InvalidTokenException("Authorization header is missing");
        }
        
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Invalid authorization header format");
        }
        
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            throw new InvalidTokenException("Token is empty");
        }
        
        if (!jwtUtils.isAccessTokenValid(token)) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        
        return token;
    }
}