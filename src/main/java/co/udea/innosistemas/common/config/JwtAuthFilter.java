package co.udea.innosistemas.common.config;

import co.udea.innosistemas.common.exception.TokenExpiredException;
import co.udea.innosistemas.common.exception.TokenRevokedException;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import co.udea.innosistemas.common.utils.JwtUtils;
import co.udea.innosistemas.common.utils.TokenValidator;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        String requestUri = request.getRequestURI();
        log.debug("Processing authentication for: {} {}", request.getMethod(), requestUri);

        try {
            processAuthentication(request);
        } catch (TokenExpiredException ex) {
            log.debug("Authentication failed - token expired: {}", ex.getMessage());
            // No establecer autenticación, dejamos que Spring Security maneje el 401
        } catch (TokenRevokedException ex) {
            log.debug("Authentication failed - token revoked: {}", ex.getMessage());
            // No establecer autenticación
        } catch (InvalidTokenException ex) {
            log.debug("Authentication failed - invalid token: {}", ex.getMessage());
            // No establecer autenticación
        } catch (Exception ex) {
            log.debug("Authentication failed - unexpected error: {}", ex.getMessage());
            // No establecer autenticación
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private void processAuthentication(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Si no hay header de autorización, continuar sin autenticación
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid authorization header found");
            return;
        }

        try {
            // Extraer y validar token
            String token = tokenValidator.validateAndExtractToken(authHeader);

            // Autenticar usuario
            authenticateUser(token, request);

        } catch (TokenExpiredException | TokenRevokedException | InvalidTokenException ex) {
            // Re-lanzar para manejo específico en el método padre
            throw ex;
        } catch (Exception ex) {
            log.warn("Unexpected error during token processing: {}", ex.getMessage());
            throw new InvalidTokenException("Error procesando el token");
        }
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            // Extraer email del token
            String email = jwtUtils.getEmailFromToken(token);

            if (!StringUtils.hasText(email)) {
                log.warn("Token does not contain valid email");
                throw new InvalidTokenException("Token no contiene email válido");
            }

            // Si ya hay autenticación, no procesarla de nuevo
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("User already authenticated in security context");
                return;
            }

            // Buscar y validar usuario
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                log.warn("User not found for email from token: {}", email);
                throw new InvalidTokenException("Usuario no encontrado");
            }

            User user = userOptional.get();

            // Verificar que el usuario esté habilitado
            if (!user.isEnabled()) {
                log.warn("Disabled user attempted to authenticate: {}", email);
                throw new InvalidTokenException("Usuario deshabilitado");
            }

            // Establecer autenticación
            setAuthentication(user, request);
            log.debug("User authenticated successfully: {}", email);

        } catch (TokenExpiredException | TokenRevokedException | InvalidTokenException ex) {
            // Re-lanzar excepciones específicas
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during user authentication", ex);
            throw new InvalidTokenException("Error interno durante la autenticación");
        }
    }

    private void setAuthentication(User user, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        try {
            // Obtener authorities del usuario
            var authorities = Optional.ofNullable(user.getAuthorities())
                    .orElseThrow(() -> new IllegalStateException("User authorities cannot be null"));

            // Crear token de autenticación
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            // Establecer detalles de la request
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Establecer en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authentication set successfully for user: {}", user.getEmail());

        } catch (Exception ex) {
            log.error("Failed to set authentication for user: {}", user.getEmail(), ex);
            throw new SecurityException("Error estableciendo la autenticación", ex);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Lista de rutas que no requieren autenticación
        String[] publicPaths = {
                "/auth/login",
                "/auth/refresh",
                "/users",           // Registro de usuarios
                "/swagger-ui",
                "/v3/api-docs",
                "/actuator",         // Si usas actuator
                "/explorer"
        };

        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                log.debug("Skipping JWT filter for public path: {}", path);
                return true;
            }
        }

        return false;
    }
}
