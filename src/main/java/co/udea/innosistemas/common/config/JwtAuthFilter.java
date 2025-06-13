package co.udea.innosistemas.common.config;

import co.udea.innosistemas.common.utils.JwtUtils;
import co.udea.innosistemas.common.utils.TokenValidator;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            processAuthentication(request);
        } catch (Exception e) {
            log.debug("Authentication failed: {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private void processAuthentication(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        try {
            String token = tokenValidator.validateAndExtractToken(authHeader);
            authenticateUser(token, request);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
        }
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        String email = jwtUtils.getEmailFromToken(token);
        
        if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        userRepository.findByEmail(email)
                .filter(User::isEnabled)  // Add enabled check
                .ifPresent(user -> {
                    if (jwtUtils.isAccessTokenValid(token)) {
                        setAuthentication(user, request);
                        log.debug("User authenticated successfully: {}", email);
                    }
                });
    }

    private void setAuthentication(User user, HttpServletRequest request) {
    if (user == null) {
        throw new IllegalArgumentException("User cannot be null");
    }
    
    try {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                Optional.ofNullable(user.getAuthorities())
                    .orElseThrow(() -> new IllegalStateException("User authorities cannot be null"))
            );
        
        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception e) {
        log.error("Failed to set authentication", e);
        throw new SecurityException("Authentication failed", e);
    }
}
}