package co.udea.innosistemas.auth.service;

import co.udea.innosistemas.auth.constants.TokenType;
import co.udea.innosistemas.auth.dto.*;
import co.udea.innosistemas.common.exception.*;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import co.udea.innosistemas.common.utils.JwtUtils;
import co.udea.innosistemas.common.exception.UserNotFoundException;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private long expiration;

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.debug("Attempting login for user: {}", request.getEmail());

        try {
            // Validar request
            validateLoginRequest(request);

            // Buscar usuario
            User user = findUserByEmail(request.getEmail());

            // Validar usuario
            validateUser(user);

            // Validar contraseña
            validatePassword(request.getPassword(), user.getPassword(), user.getEmail());

            // Generar tokens
            String userRole = user.getRole() != null ? user.getRole().getName() : null;
            String accessToken = jwtUtils.generateToken(user.getEmail(), userRole);
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), userRole);

            log.info("Successful login for user: {}", request.getEmail());

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiration / 1000)
                    .build();

        } catch (UserNotFoundException | UserDisabledException |
                 AuthenticationException ex) {
            // Re-lanzar excepciones específicas
            throw ex;
        } catch (BadCredentialsException ex) {
            // Convertir BadCredentialsException a nuestra excepción específica
            throw new AuthenticationException(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during login for user: {}", request.getEmail(), ex);
            throw new AuthenticationException(
                    "Error interno durante la autenticación", ex);
        }
    }

    public UserProfileResponseDTO getProfile(User user) {
        log.debug("Fetching profile for user: {}", user.getEmail());

        try {
            if (user == null) {
                throw new InvalidTokenException("Usuario no válido");
            }

            return UserProfileResponseDTO.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .roleId(user.getRole() != null ? user.getRole().getId().toString() : null)
                    .roleName(user.getRole() != null ? user.getRole().getName() : null)
                    .courseId(user.getCourse() != null ? user.getCourse().getId().toString() : null)
                    .courseName(user.getCourse() != null ? user.getCourse().getName() : null)
                    .team(user.getTeam() != null
                            ? new UserProfileResponseDTO.TeamDto(user.getTeam().getId(), user.getTeam().getName())
                            : null)
                    .build();
        } catch (Exception ex) {
            log.error("Error fetching profile for user: {}", user.getEmail(), ex);
            throw new co.udea.innosistemas.common.exception.AuthenticationException(
                    "Error al obtener el perfil del usuario", ex);
        }
    }

    @Transactional
    public LogoutResponseDTO logout(String accessToken, TokenRevokeRequestDTO revokeRequest) {
        log.debug("Processing logout request");

        try {
            validateLogoutRequest(accessToken);

            boolean accessRevoked = false;
            boolean refreshRevoked = false;

            // Revocar access token
            try {
                jwtUtils.revokeToken(accessToken, TokenType.ACCESS.getValue());
                accessRevoked = true;
                log.debug("Access token revoked successfully");
            } catch (Exception ex) {
                log.warn("Failed to revoke access token", ex);
                throw new LogoutException("Error al revocar el token de acceso");
            }

            // Revocar refresh token si se proporciona
            if (revokeRequest != null && StringUtils.hasText(revokeRequest.getRefreshToken())) {
                try {
                    jwtUtils.revokeToken(revokeRequest.getRefreshToken(), TokenType.REFRESH.getValue());
                    refreshRevoked = true;
                    log.debug("Refresh token revoked successfully");
                } catch (Exception ex) {
                    log.warn("Failed to revoke refresh token", ex);
                    // No lanzamos excepción aquí porque el access token ya se revocó
                }
            }

            log.info("Logout successful. Access token revoked: {}, Refresh token revoked: {}",
                    accessRevoked, refreshRevoked);

            return LogoutResponseDTO.builder()
                    .message("Sesión cerrada correctamente")
                    .accessTokenRevoked(accessRevoked)
                    .refreshTokenRevoked(refreshRevoked)
                    .build();

        } catch (LogoutException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during logout process", ex);
            throw new LogoutException("Error interno durante el cierre de sesión", ex);
        }
    }

    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken) {
        log.debug("Processing refresh token request");

        try {
            validateRefreshTokenRequest(refreshToken);

            // Validar refresh token
            if (jwtUtils.isRefreshTokenValid(refreshToken)) {
                log.warn("Invalid refresh token attempt");
                throw new RefreshTokenException("Refresh token inválido, expirado o revocado");
            }

            // Obtener email del token
            String email = jwtUtils.getEmailFromToken(refreshToken);
            if (!StringUtils.hasText(email)) {
                throw new RefreshTokenException("No se pudo extraer el email del refresh token");
            }

            // Buscar usuario
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado para el refresh token"));

            // Validar que el usuario esté habilitado
            if (!user.isEnabled()) {
                throw new UserDisabledException("La cuenta del usuario está deshabilitada");
            }

            // Generar nuevos tokens
            String userRole = user.getRole() != null ? user.getRole().getName() : null;
            String newAccessToken = jwtUtils.generateToken(email, userRole);
            String newRefreshToken = jwtUtils.generateRefreshToken(email, userRole);

            // Revocar el refresh token anterior
            try {
                jwtUtils.revokeToken(refreshToken, TokenType.REFRESH.getValue());
            } catch (Exception ex) {
                log.warn("Failed to revoke old refresh token during refresh", ex);
                // Continuamos porque los nuevos tokens ya están generados
            }

            log.info("Token refresh successful for user: {}", email);

            return AuthResponseDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtils.getExpirationInSeconds())
                    .build();

        } catch (RefreshTokenException | UserNotFoundException | UserDisabledException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during token refresh", ex);
            throw new RefreshTokenException("Error interno durante la renovación del token", ex);
        }
    }

    // Métodos de validación privados
    private void validateLoginRequest(LoginRequestDTO request) {
        if (request == null) {
            throw new AuthenticationException(
                    "La solicitud de login no puede ser nula");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new AuthenticationException(
                    "El correo electrónico es obligatorio");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new AuthenticationException(
                    "La contraseña es obligatoria");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt with non-existent email: {}", email);
                    return new AuthenticationException("Credenciales inválidas");
                });
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new AuthenticationException("Usuario no válido");
        }

        if (!user.isEnabled()) {
            log.warn("Login attempt by disabled user: {}", user.getEmail());
            throw new UserDisabledException("La cuenta del usuario está deshabilitada");
        }
    }

    private void validatePassword(String providedPassword, String storedPassword, String email) {
        if (!passwordEncoder.matches(providedPassword, storedPassword)) {
            log.warn("Invalid password attempt for user: {}", email);
            throw new AuthenticationException("Credenciales inválidas");
        }
    }

    private void validateLogoutRequest(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new LogoutException("Token de acceso requerido para el logout");
        }
    }

    private void validateRefreshTokenRequest(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new RefreshTokenException("Refresh token es obligatorio");
        }
    }
}