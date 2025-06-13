package co.udea.innosistemas.auth.service;

import co.udea.innosistemas.auth.constants.TokenType;
import co.udea.innosistemas.auth.dto.*;
import co.udea.innosistemas.common.exception.InvalidTokenException;
import co.udea.innosistemas.common.utils.JwtUtils;
import co.udea.innosistemas.user.model.User;
import co.udea.innosistemas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        validateUser(user);  // Add this line to check if user is enabled

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for user: {}", request.getEmail());
            throw new BadCredentialsException("Contraseña inválida");
        }

        String userRole = user.getRole() != null ? user.getRole().getName() : null; //Added to get the current user role name
        String accessToken = jwtUtils.generateToken(user.getEmail(), userRole); //Now generateToken asks for userRole
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), userRole); //Now generateRefreshToken asks for userRole

        log.info("Successful login for user: {}", request.getEmail());
        
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration / 1000)
                .build();
    }

    public UserProfileResponseDTO getProfile(User user) {
        log.debug("Fetching profile for user: {}", user.getEmail());
        
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
    }

    @Transactional
    public LogoutResponseDTO logout(String accessToken, TokenRevokeRequestDTO revokeRequest) {
        log.debug("Processing logout request");
        
        boolean accessRevoked;
        boolean refreshRevoked = false;

        try {
            jwtUtils.revokeToken(accessToken, TokenType.ACCESS.getValue());
            accessRevoked = true;

            if (revokeRequest != null && revokeRequest.getRefreshToken() != null) {
                jwtUtils.revokeToken(revokeRequest.getRefreshToken(), TokenType.REFRESH.getValue());
                refreshRevoked = true;
            }

            log.info("Logout successful. Access token revoked: {}, Refresh token revoked: {}", 
                    accessRevoked, refreshRevoked);

            return LogoutResponseDTO.builder()
                    .message("Sesión cerrada correctamente")
                    .accessTokenRevoked(accessRevoked)
                    .refreshTokenRevoked(refreshRevoked)
                    .build();

        } catch (Exception e) {
            log.error("Error during logout process", e);
            throw new InvalidTokenException("Error al procesar el cierre de sesión");
        }
    }

    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken) {
        log.debug("Processing refresh token request");
        
        if (!jwtUtils.isRefreshTokenValid(refreshToken)) {
            log.warn("Invalid refresh token attempt");
            throw new InvalidTokenException("Refresh token inválido o revocado");
        }

        String email = jwtUtils.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));

        String userRole = user.getRole() != null ? user.getRole().getName() : null;
        String newAccessToken = jwtUtils.generateToken(email, userRole);
        String newRefreshToken = jwtUtils.generateRefreshToken(email, userRole);

        // Revoke old refresh token
        jwtUtils.revokeToken(refreshToken, TokenType.REFRESH.getValue());

        log.info("Token refresh successful for user: {}", email);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .build();
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new InvalidTokenException("Usuario no válido");
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Usuario deshabilitado");
        }
    }
}